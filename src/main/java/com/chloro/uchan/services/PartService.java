package com.chloro.uchan.services;

import com.chloro.uchan.enums.Slot;
import com.chloro.uchan.enums.SlotAC6;
import com.chloro.uchan.json.Part;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PartService {

    private final Map<Slot, List<Part>> parts;

    public PartService() throws Exception {
        File partsFile = new ClassPathResource("PARTS_AC6_1_07_2.json").getFile();
        parts = Arrays.stream(new ObjectMapper().readValue(partsFile, Part[].class))
                .map(part -> part.getSlots().stream()
                        .map(slot -> new AbstractMap.SimpleEntry<>(slot, part))
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        Collectors.mapping(AbstractMap.SimpleEntry::getValue, Collectors.toList())));
    }

    public String distribution() {
        List<Slot> slotList = List.of(SlotAC6.values());
        long trials = 50000;

        long failures = 0;
        Map<Part, Long> distribution = new HashMap<>();
        for (int ii = 0; ii < trials; ii++) {
            try {
                smartRandomBuildAC6(1000).forEach((key, value) -> {
                    if (distribution.containsKey(value)) {
                        distribution.put(value, distribution.get(value) + 1);
                    } else {
                        distribution.put(value, 1L);
                    }
                });
            } catch (Exception ex) {
                failures++;
            }
        }

        StringBuilder distributionString = new StringBuilder();
        List<Double> ratios = new ArrayList<>();
        for (Slot slot : slotList) {
            distributionString.append(String.format("**%s:**", slot));
            Map.Entry<Part, Long> min = distribution.entrySet().stream()
                    .filter(entry -> entry.getKey().getType() != null) // filter out (NOTHING)
                    .filter(entry -> entry.getKey().getSlots().contains(slot))
                    .min(Map.Entry.comparingByValue())
                    .orElseThrow();
            distributionString.append(String.format("\n- min: **%s** %s", min.getValue(), min.getKey().getName()));
            Map.Entry<Part, Long> max = distribution.entrySet().stream()
                    .filter(entry -> entry.getKey().getType() != null) // filter out (NOTHING)
                    .filter(entry -> entry.getKey().getSlots().contains(slot))
                    .max(Map.Entry.comparingByValue())
                    .orElseThrow();
            distributionString.append(String.format("\n- max: **%s** %s", max.getValue(), max.getKey().getName()));
            Double ratio = (double) max.getValue() / min.getValue();
            ratios.add(ratio);
            distributionString.append(String.format("\n- ratio: **%4.1f**\n\n", ratio));
        }
        distributionString.append("**Totals:**");
        Double min = ratios.stream().min(Double::compareTo).orElseThrow();
        distributionString.append(String.format("\n- min ratio: **%4.1f**", min));
        Double avg = ratios.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        distributionString.append(String.format("\n- avg ratio: **%4.1f**", avg));
        Double max = ratios.stream().max(Double::compareTo).orElseThrow();
        distributionString.append(String.format("\n- max ratio: **%4.1f**", max));

        return String.format("After %s trials, the distribution was:\n\n%s failures!\n\n%s",
                trials, failures, distributionString);
    }

    public String randomBuild() {
        Map<Slot, Part> build;
        try {
            build = smartRandomBuildAC6(10000);
        } catch (Exception ex) {
            return ex.getMessage();
        }

        String buildString = build.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getOrdinal()))
                .map(entry -> String.format(
                        "**%s**: %s",
                        entry.getKey(),
                        entry.getValue().getName()))
                .collect(Collectors.joining("\n"));

        return String.format("Here is your random build:\n%s", buildString);
    }

    private Map<Slot, Part> smartRandomBuildAC6(long attemptsPerStep) throws Exception {
        Map<Slot, Part> build = defaultAssemblyAC6(EnumSet.of(SlotAC6.HEAD)); // TODO: Make "build" into a class Build.

        // Mix up the order here to reduce bias.
        if (new Random().nextBoolean()) {
            randomizeWeaponsAC6(build, true, true, true);
            simpleRandomizeAC6(build, EnumSet.of(SlotAC6.LEGS, SlotAC6.GENERATOR), true, attemptsPerStep);
            simpleRandomizeAC6(build, EnumSet.of(SlotAC6.CORE), true, attemptsPerStep);
        } else {
            simpleRandomizeAC6(build, EnumSet.of(SlotAC6.LEGS, SlotAC6.GENERATOR), false, 1);
            simpleRandomizeAC6(build, EnumSet.of(SlotAC6.CORE), true, attemptsPerStep);
            boolean valid = false;
            for (long ll = 0; !valid && ll < attemptsPerStep; ll++) {
                randomizeWeaponsAC6(build, true, true, true);
                valid = validateForAc6(convertForAC6(build));
            }
            if (!valid) {
                throw new Exception("Failed to generate a valid random assembly!");
            }
        }
        simpleRandomizeAC6(build, EnumSet.of(SlotAC6.BOOSTER), true, attemptsPerStep);
        simpleRandomizeAC6(build, EnumSet.of(SlotAC6.HEAD, SlotAC6.FCS, SlotAC6.EXPANSION), true, attemptsPerStep);

        return build;
    }

    private void simpleRandomizeAC6(Map<Slot, Part> build, Set<SlotAC6> slots, boolean validate, long attemptsPerStep) throws Exception {
        boolean valid = false;
        for (long ll = 0; !valid && ll < attemptsPerStep; ll++) {
            slots.forEach(slot -> {
                if ((slot.equals(SlotAC6.BOOSTER) && build.get(SlotAC6.LEGS).getType().equals("TANK"))) { // TODO: enum
                    build.put(SlotAC6.BOOSTER, parts.values().stream()
                            .flatMap(List::stream)
                            .filter(part -> part.getCategory() == null)
                            .findFirst()
                            .orElseThrow());
                    return;
                }
                build.put(slot, randomPart(parts.get(slot)));
            });
            valid = !validate || validateForAc6(convertForAC6(build));
        }
        if (validate && !valid) {
            throw new Exception("Failed to generate a valid random assembly!");
        }
    }

    private void randomizeWeaponsAC6(Map<Slot, Part> build,
                                     boolean randomizeSlotCombination,
                                     boolean weighted,
                                     boolean randomizeValidArms) {
        Set<SlotAC6> weaponSlots = EnumSet.of(
                SlotAC6.L_BACK_UNIT, SlotAC6.L_ARM_UNIT,
                SlotAC6.R_BACK_UNIT, SlotAC6.R_ARM_UNIT);
        weaponSlots = weaponSlots.stream()
                .sorted(Comparator.comparing(SlotAC6::getOrdinal).reversed())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        weaponSlots.forEach(weaponSlot -> {
            List<Slot> filter = randomizeSlotCombination ? getSlotFilter(weaponSlot, weighted) : null;
            assert filter != null;
            build.put(weaponSlot, randomPart(parts.get(weaponSlot).stream()
                    .filter(part -> !randomizeSlotCombination || part.getSlots().equals(filter))
                    .filter(part -> !(weaponSlot == SlotAC6.L_ARM_UNIT
                                                && part.equals(build.get(SlotAC6.L_BACK_UNIT)))
                                        && !(weaponSlot == SlotAC6.R_ARM_UNIT
                                                && part.equals(build.get(SlotAC6.R_BACK_UNIT))))
                    .toList()));
        });
        if (randomizeValidArms) {
            Long weightRight = build.entrySet().stream()
                    .filter(entry ->
                            entry.getKey().equals(SlotAC6.R_ARM_UNIT) || entry.getKey().equals(SlotAC6.R_BACK_UNIT))
                    .map(Map.Entry::getValue)
                    .filter(part -> part.getSlots().contains(SlotAC6.R_ARM_UNIT))
                    .max(Comparator.comparing(Part::getWeight))
                    .orElseThrow()
                    .getWeight();
            Long weightLeft = build.entrySet().stream()
                    .filter(entry ->
                            entry.getKey().equals(SlotAC6.L_ARM_UNIT) || entry.getKey().equals(SlotAC6.L_BACK_UNIT))
                    .map(Map.Entry::getValue)
                    .filter(part -> part.getSlots().contains(SlotAC6.L_ARM_UNIT))
                    .max(Comparator.comparing(Part::getWeight))
                    .orElseThrow()
                    .getWeight();
            List<Part> validArms = parts.values().stream().flatMap(List::stream)
                    .filter(part -> part.getArmLoadLimit() != null)
                    .filter(part -> part.getArmLoadLimit() > weightRight + weightLeft)
                    .toList();
            build.put(SlotAC6.ARMS, validArms.get(new Random().nextInt(validArms.size())));
        }
    }

    private List<Slot> getSlotFilter(Slot slot, boolean weighted) {
        List<List<Slot>> options = parts.get(slot).stream()
                // TODO: Param this to allow empty parts.
                .filter(part -> part.getCategory() != null)
                .map(Part::getSlots)
                .distinct()
                .toList();

        if (weighted) {
            List<List<Slot>> weightedOptions = new ArrayList<>(options);
            Map<List<Slot>, Long> weights = slotWeights();
            for (List<Slot> option : options) {
                Long weight = weights.get(option);
                for (long ll = weight; ll > 1; ll--) { // Each option is already in the list once.
                    weightedOptions.add(option);
                }
            }
            return weightedOptions.get(new Random().nextInt(weightedOptions.size()));
        } else {
            return options.get(new Random().nextInt(options.size()));
        }
    }

    private Map<List<Slot>, Long> slotWeights() {
        Map<Slot, List<List<Slot>>> optionsPerSlot = parts.values().stream()
                .flatMap(List::stream)
                .filter(part -> part.getCategory() != null)
                .map(Part::getSlots)
                .distinct()
                .flatMap(slots -> slots.stream().map(slot -> new AbstractMap.SimpleEntry<>(slot, slots)))
                .collect(Collectors.toMap(
                        AbstractMap.SimpleEntry::getKey,
                        entry -> List.of(entry.getValue()),
                        (oo, nn) -> {
                            List<List<Slot>> combined = new ArrayList<>(oo);
                            combined.addAll(nn);
                            return combined;
                        }));

        Map<List<Slot>, List<Integer>> denominatorsPerOption = optionsPerSlot.values().stream()
                .flatMap(options ->
                        options.stream().map(option ->
                                new AbstractMap.SimpleEntry<>(option, options.size())))
                .collect(Collectors.toMap(
                        AbstractMap.SimpleEntry::getKey,
                        entry -> List.of(entry.getValue()),
                        (oo, nn) -> {
                            List<Integer> combined = new ArrayList<>(oo);
                            combined.addAll(nn);
                            return combined;
                        }));

        Double target = denominatorsPerOption.values().stream()
                .map(this::naturalWeight)
                .max(Double::compareTo)
                .orElseThrow();

        Map<List<Slot>, Long> partsPerOption = parts.values().stream()
                .flatMap(Collection::stream)
                .filter(part -> part.getCategory() != null)
                .distinct()
                .collect(Collectors.groupingBy(Part::getSlots, Collectors.counting()));

        return partsPerOption.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(
                        entry.getKey(),
                        entry.getValue()
                                * Math.round(target / naturalWeight(denominatorsPerOption.get(entry.getKey())))))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private Double naturalWeight(List<Integer> denominators) {
        return denominators.stream().map(val -> 1d / val).mapToDouble(Double::doubleValue).sum();
    }

    private Part randomPart(List<Part> parts) {
        List<Part> partList = parts.stream()
                // TODO: Param this to allow empty parts.
                .filter(part -> part.getCategory() != null)
                .toList();

        return partList.get(new Random().nextInt(partList.size()));
    }

    private Map<Slot, Part> defaultAssemblyAC6(Set<SlotAC6> slotsToReverse) {
        List<Slot> slotList = List.of(SlotAC6.values());

        Map<Slot, Part> assembly = slotList.stream()
                .map(slot -> {
                    Part cheapestPart = parts.get(slot).stream()
                            // TODO: Param this to allow empty parts.
                            .filter(part -> part.getCategory() != null)
                            .filter(part -> part.getWeight() != null)
                            .min(Comparator.comparing(Part::getWeight))
                            .orElse(parts.get(slot).getFirst());
                    return new AbstractMap.SimpleEntry<>(slot, cheapestPart);})
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Slot slot : slotsToReverse) {
            assembly.put(slot, parts.get(slot).stream()
                    .filter(part -> part.getWeight() != null)
                    .max(Comparator.comparing(Part::getWeight))
                    .orElseThrow());
        }

        assembly.put(SlotAC6.LEGS, parts.get(SlotAC6.LEGS).stream()
                .max(Comparator.comparing(Part::getLoadLimit))
                .orElseThrow());
        assembly.put(SlotAC6.GENERATOR, parts.get(SlotAC6.GENERATOR).stream()
                .max(Comparator.comparing(Part::getEnOutput))
                .orElseThrow());
        assembly.put(SlotAC6.CORE, parts.get(SlotAC6.CORE).stream()
                .max(Comparator.comparing(Part::getEnAdjust))
                .orElseThrow());

        return assembly;
    }

    private Map<SlotAC6, Part> convertForAC6(Map<Slot, Part> build) {
        return build.entrySet().stream()
                .map(entry -> {
                    if(entry.getKey() instanceof SlotAC6) {
                        return new AbstractMap.SimpleEntry<>((SlotAC6)entry.getKey(), entry.getValue());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private boolean validateForAc6(Map<SlotAC6, Part> build) {
        Long loadLimit = build.get(SlotAC6.LEGS).getLoadLimit();
        Long enOutput = build.get(SlotAC6.GENERATOR).getEnOutput();
        Long enAdjust = build.get(SlotAC6.CORE).getEnAdjust();
        Double enScalar = enAdjust * 0.01;
        long enLimit = (long)Math.floor(enOutput * enScalar);

        Long load = build.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(SlotAC6.LEGS))
                .map(entry -> entry.getValue().getWeight())
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);

        Long enLoad = build.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(SlotAC6.GENERATOR))
                .map(entry -> entry.getValue().getEnLoad())
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);

        return load <= loadLimit && enLoad <= enLimit;
        /* TODO: We should probably add this here, but the CORE step requires that we validate for everything but this.
         * So for the meantime let's just leave these out until we have an elegant solution. */
//                && !build.get(SlotAC6.R_ARM_UNIT).equals(build.get(SlotAC6.R_BACK_UNIT))
//                && !build.get(SlotAC6.L_ARM_UNIT).equals(build.get(SlotAC6.L_BACK_UNIT));
    }
}
