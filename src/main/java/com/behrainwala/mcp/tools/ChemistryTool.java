package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * MCP tool for university-level chemistry calculations.
 * Covers stoichiometry, solutions, thermochemistry, gas laws, electrochemistry,
 * kinetics, equilibrium, and the periodic table.
 */
@Service
public class ChemistryTool {

    private static final MathContext MC = new MathContext(10);
    private static final double R_GAS = 8.314462618;    // J/(mol⋅K) or L⋅kPa/(mol⋅K)
    private static final double R_ATM = 0.082057;       // L⋅atm/(mol⋅K)
    private static final double FARADAY = 96485.33212;   // C/mol
    private static final double N_A = 6.02214076e23;

    // Periodic table data: symbol → [atomic_number, atomic_mass, name, group, period, category]
    private static final Map<String, ElementData> PERIODIC_TABLE = new LinkedHashMap<>();

    static {
        addElement("H", 1, 1.008, "Hydrogen", 1, 1, "Nonmetal");
        addElement("He", 2, 4.003, "Helium", 18, 1, "Noble gas");
        addElement("Li", 3, 6.941, "Lithium", 1, 2, "Alkali metal");
        addElement("Be", 4, 9.012, "Beryllium", 2, 2, "Alkaline earth");
        addElement("B", 5, 10.81, "Boron", 13, 2, "Metalloid");
        addElement("C", 6, 12.011, "Carbon", 14, 2, "Nonmetal");
        addElement("N", 7, 14.007, "Nitrogen", 15, 2, "Nonmetal");
        addElement("O", 8, 15.999, "Oxygen", 16, 2, "Nonmetal");
        addElement("F", 9, 18.998, "Fluorine", 17, 2, "Halogen");
        addElement("Ne", 10, 20.180, "Neon", 18, 2, "Noble gas");
        addElement("Na", 11, 22.990, "Sodium", 1, 3, "Alkali metal");
        addElement("Mg", 12, 24.305, "Magnesium", 2, 3, "Alkaline earth");
        addElement("Al", 13, 26.982, "Aluminum", 13, 3, "Post-transition metal");
        addElement("Si", 14, 28.086, "Silicon", 14, 3, "Metalloid");
        addElement("P", 15, 30.974, "Phosphorus", 15, 3, "Nonmetal");
        addElement("S", 16, 32.065, "Sulfur", 16, 3, "Nonmetal");
        addElement("Cl", 17, 35.453, "Chlorine", 17, 3, "Halogen");
        addElement("Ar", 18, 39.948, "Argon", 18, 3, "Noble gas");
        addElement("K", 19, 39.098, "Potassium", 1, 4, "Alkali metal");
        addElement("Ca", 20, 40.078, "Calcium", 2, 4, "Alkaline earth");
        addElement("Ti", 22, 47.867, "Titanium", 4, 4, "Transition metal");
        addElement("Cr", 24, 51.996, "Chromium", 6, 4, "Transition metal");
        addElement("Mn", 25, 54.938, "Manganese", 7, 4, "Transition metal");
        addElement("Fe", 26, 55.845, "Iron", 8, 4, "Transition metal");
        addElement("Co", 27, 58.933, "Cobalt", 9, 4, "Transition metal");
        addElement("Ni", 28, 58.693, "Nickel", 10, 4, "Transition metal");
        addElement("Cu", 29, 63.546, "Copper", 11, 4, "Transition metal");
        addElement("Zn", 30, 65.380, "Zinc", 12, 4, "Transition metal");
        addElement("Br", 35, 79.904, "Bromine", 17, 4, "Halogen");
        addElement("Kr", 36, 83.798, "Krypton", 18, 4, "Noble gas");
        addElement("Ag", 47, 107.868, "Silver", 11, 5, "Transition metal");
        addElement("I", 53, 126.904, "Iodine", 17, 5, "Halogen");
        addElement("Ba", 56, 137.327, "Barium", 2, 6, "Alkaline earth");
        addElement("Au", 79, 196.967, "Gold", 11, 6, "Transition metal");
        addElement("Hg", 80, 200.592, "Mercury", 12, 6, "Transition metal");
        addElement("Pb", 82, 207.200, "Lead", 14, 6, "Post-transition metal");
        addElement("U", 92, 238.029, "Uranium", 3, 7, "Actinide");
    }

    private static void addElement(String symbol, int number, double mass, String name, int group, int period, String category) {
        PERIODIC_TABLE.put(symbol.toLowerCase(), new ElementData(symbol, number, mass, name, group, period, category));
    }

    record ElementData(String symbol, int number, double mass, String name, int group, int period, String category) {}

    @Tool(name = "element_lookup", description = "Look up an element in the periodic table by symbol or name. "
            + "Returns atomic number, mass, group, period, and category.")
    public String elementLookup(
            @ToolParam(description = "Element symbol (e.g. 'Fe', 'Na') or name (e.g. 'Iron', 'Sodium')") String element) {

        String q = element.strip().toLowerCase();

        // Search by symbol or name
        ElementData data = PERIODIC_TABLE.get(q);
        if (data == null) {
            for (ElementData e : PERIODIC_TABLE.values()) {
                if (e.name.toLowerCase().equals(q)) { data = e; break; }
            }
        }

        if (data == null) {
            return "Element '" + element + "' not found. Try using the chemical symbol (e.g. 'Fe') or full name (e.g. 'Iron').";
        }

        return String.format("""
                        Element: %s (%s)
                        ──────────────────
                          Atomic number: %d
                          Atomic mass: %.3f u
                          Group: %d
                          Period: %d
                          Category: %s
                          Molar mass: %.3f g/mol""",
                data.name, data.symbol, data.number, data.mass,
                data.group, data.period, data.category, data.mass);
    }

    @Tool(name = "molar_mass", description = "Calculate the molar mass of a chemical compound from its formula. "
            + "Supports parentheses and subscripts. Examples: 'H2O', 'Ca(OH)2', 'C6H12O6', 'Fe2O3'.")
    public String molarMass(
            @ToolParam(description = "Chemical formula (e.g. 'H2O', 'NaCl', 'Ca(OH)2', 'C6H12O6')") String formula) {

        try {
            Map<String, Double> composition = parseFormula(formula.strip(), 1);
            double totalMass = 0;

            StringBuilder sb = new StringBuilder();
            sb.append("Molar Mass: ").append(formula).append("\n");
            sb.append("──────────────────\n\n");
            sb.append("COMPOSITION\n");

            for (Map.Entry<String, Double> entry : composition.entrySet()) {
                String sym = entry.getKey();
                double count = entry.getValue();
                ElementData elem = PERIODIC_TABLE.get(sym.toLowerCase());
                if (elem == null) {
                    return "Error: Unknown element '" + sym + "' in formula.";
                }
                double mass = elem.mass * count;
                totalMass += mass;
                sb.append(String.format("  %s: %.3f u × %.0f = %.3f g/mol\n", sym, elem.mass, count, mass));
            }

            sb.append(String.format("\nMolar Mass = %.3f g/mol\n", totalMass));
            sb.append(String.format("Mass of 1 molecule = %.4e g", totalMass / N_A));

            // Mass percentages
            sb.append("\n\nMASS PERCENTAGES\n");
            for (Map.Entry<String, Double> entry : composition.entrySet()) {
                ElementData elem = PERIODIC_TABLE.get(entry.getKey().toLowerCase());
                double mass = elem.mass * entry.getValue();
                sb.append(String.format("  %s: %.2f%%\n", entry.getKey(), (mass / totalMass) * 100));
            }

            return sb.toString();

        } catch (Exception e) {
            return "Error parsing formula '" + formula + "': " + e.getMessage();
        }
    }

    @Tool(name = "chemistry_solutions", description = "Solve solution chemistry problems: molarity, dilution, "
            + "pH/pOH, mole calculations, mass-to-moles conversion, and stoichiometry.")
    public String solutions(
            @ToolParam(description = "Formula: 'molarity' (M=n/V), 'dilution' (M1V1=M2V2), "
                    + "'ph' (pH=-log[H+]), 'poh' (pOH=-log[OH-]), 'ph_to_h' (convert pH to [H+]), "
                    + "'mass_to_moles' (n=m/M), 'moles_to_mass' (m=nM), "
                    + "'moles_to_particles' (N=nNa), 'percent_composition' (mass% = part/total × 100)") String formula,
            @ToolParam(description = """
                    Comma-separated values for the formula:
                      molarity: moles, volume(L)
                      dilution: M1, V1, M2_or_V2 (provide 3 to solve for 4th)
                      ph: H+ concentration (mol/L)
                      poh: OH- concentration (mol/L)
                      ph_to_h: pH value
                      mass_to_moles: mass(g), molar_mass(g/mol)
                      moles_to_mass: moles, molar_mass(g/mol)
                      moles_to_particles: moles
                      percent_composition: part_mass, total_mass""") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "molarity" -> {
                    check(v, 2, "moles, volume(L)");
                    double M = v[0] / v[1];
                    yield phResult("Molarity", "M = n/V",
                            "M = " + fmt(v[0]) + " / " + fmt(v[1]) + " = " + fmt(M) + " mol/L (M)",
                            "n = " + fmt(v[0]) + " mol, V = " + fmt(v[1]) + " L");
                }
                case "dilution" -> {
                    check(v, 3, "M1, V1, V2 (solves for M2) or M1, V1, M2 (solves for V2)");
                    double M1V1 = v[0] * v[1];
                    double M2 = M1V1 / v[2];
                    double V2 = M1V1 / v[2];
                    yield phResult("Dilution", "M₁V₁ = M₂V₂",
                            "M₁V₁ = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(M1V1) + "\n"
                                    + "If V₂ = " + fmt(v[2]) + ": M₂ = " + fmt(M2) + " M\n"
                                    + "If M₂ = " + fmt(v[2]) + ": V₂ = " + fmt(V2) + " L",
                            "M₁ = " + fmt(v[0]) + " M, V₁ = " + fmt(v[1]) + " L, third value = " + fmt(v[2]));
                }
                case "ph" -> {
                    check(v, 1, "[H+] concentration");
                    double pH = -Math.log10(v[0]);
                    double pOH = 14 - pH;
                    double oh = Math.pow(10, -pOH);
                    yield phResult("pH Calculation", "pH = -log[H⁺]",
                            "pH = " + fmt(pH) + "\npOH = " + fmt(pOH) + "\n[OH⁻] = " + sci(oh) + " M\n"
                                    + "Solution is " + (pH < 7 ? "acidic" : pH > 7 ? "basic" : "neutral"),
                            "[H⁺] = " + sci(v[0]) + " M");
                }
                case "poh" -> {
                    check(v, 1, "[OH-] concentration");
                    double pOH = -Math.log10(v[0]);
                    double pH = 14 - pOH;
                    yield phResult("pOH Calculation", "pOH = -log[OH⁻]",
                            "pOH = " + fmt(pOH) + "\npH = " + fmt(pH),
                            "[OH⁻] = " + sci(v[0]) + " M");
                }
                case "ph_to_h" -> {
                    check(v, 1, "pH value");
                    double h = Math.pow(10, -v[0]);
                    double pOH = 14 - v[0];
                    double oh = Math.pow(10, -pOH);
                    yield phResult("pH → Concentration", "[H⁺] = 10^(-pH)",
                            "[H⁺] = " + sci(h) + " M\n[OH⁻] = " + sci(oh) + " M\npOH = " + fmt(pOH),
                            "pH = " + fmt(v[0]));
                }
                case "mass_to_moles" -> {
                    check(v, 2, "mass(g), molar_mass(g/mol)");
                    double n = v[0] / v[1];
                    double particles = n * N_A;
                    yield phResult("Mass to Moles", "n = m/M",
                            "n = " + fmt(v[0]) + " / " + fmt(v[1]) + " = " + fmt(n) + " mol\n"
                                    + "Number of particles = " + sci(particles),
                            "m = " + fmt(v[0]) + " g, M = " + fmt(v[1]) + " g/mol");
                }
                case "moles_to_mass" -> {
                    check(v, 2, "moles, molar_mass(g/mol)");
                    double m = v[0] * v[1];
                    yield phResult("Moles to Mass", "m = nM",
                            "m = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(m) + " g",
                            "n = " + fmt(v[0]) + " mol, M = " + fmt(v[1]) + " g/mol");
                }
                case "moles_to_particles" -> {
                    check(v, 1, "moles");
                    double N = v[0] * N_A;
                    yield phResult("Moles to Particles", "N = nNₐ",
                            "N = " + sci(N) + " particles",
                            "n = " + fmt(v[0]) + " mol, Nₐ = " + sci(N_A) + " mol⁻¹");
                }
                case "percent_composition" -> {
                    check(v, 2, "part_mass, total_mass");
                    double pct = (v[0] / v[1]) * 100;
                    yield phResult("Percent Composition", "% = (part/total) × 100",
                            "Percentage = " + fmt(pct) + "%",
                            "Part = " + fmt(v[0]) + " g, Total = " + fmt(v[1]) + " g");
                }
                default -> "Unknown formula. Use: molarity, dilution, ph, poh, ph_to_h, "
                        + "mass_to_moles, moles_to_mass, moles_to_particles, percent_composition";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "chemistry_gas_laws", description = "Solve gas law problems: ideal gas law, combined gas law, "
            + "Boyle's, Charles's, Avogadro's, Dalton's partial pressures, and Graham's effusion.")
    public String gasLaws(
            @ToolParam(description = "Formula: 'ideal_gas' (PV=nRT), 'combined' (P1V1/T1=P2V2/T2), "
                    + "'boyle' (P1V1=P2V2), 'charles' (V1/T1=V2/T2), 'gay_lussac' (P1/T1=P2/T2), "
                    + "'dalton' (P_total=P1+P2+...), 'graham' (rate1/rate2=√(M2/M1)), "
                    + "'density' (d=PM/RT)") String formula,
            @ToolParam(description = "Comma-separated values. For ideal_gas use: P(atm), V(L), n(mol), T(K) — "
                    + "provide 3 to solve for the 4th. For combined: P1,V1,T1,P2,V2 (solves T2) etc.") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "ideal_gas" -> {
                    // PV = nRT with R = 0.08206 L⋅atm/(mol⋅K)
                    check(v, 3, "3 of: P(atm), V(L), n(mol), T(K)");
                    if (v.length == 4) {
                        // All 4 given — verify
                        double check = v[0] * v[1] / (v[2] * v[3]);
                        yield phResult("Ideal Gas Law (Verification)", "PV = nRT",
                                "PV/(nT) = " + fmt(check) + " (should be ~0.08206 L⋅atm/(mol⋅K))\n"
                                        + (Math.abs(check - R_ATM) < 0.01 ? "✓ Consistent" : "✗ Inconsistent — check values"),
                                "P=" + fmt(v[0]) + " atm, V=" + fmt(v[1]) + " L, n=" + fmt(v[2]) + " mol, T=" + fmt(v[3]) + " K");
                    }
                    // Solve for 4th (assume last is unknown based on 3 values)
                    double P = v[0], V2 = v[1], third = v[2];
                    yield phResult("Ideal Gas Law", "PV = nRT",
                            "If n=" + fmt(third) + " mol: T = PV/(nR) = " + fmt(P * V2 / (third * R_ATM)) + " K\n"
                                    + "If T=" + fmt(third) + " K: n = PV/(RT) = " + fmt(P * V2 / (R_ATM * third)) + " mol",
                            "P=" + fmt(P) + " atm, V=" + fmt(V2) + " L, 3rd value=" + fmt(third));
                }
                case "boyle" -> {
                    check(v, 3, "P1, V1, P2_or_V2");
                    double P2 = v[0] * v[1] / v[2];
                    double V2 = v[0] * v[1] / v[2];
                    yield phResult("Boyle's Law", "P₁V₁ = P₂V₂ (constant T, n)",
                            "If P₂ = " + fmt(v[2]) + ": V₂ = " + fmt(V2) + "\n"
                                    + "If V₂ = " + fmt(v[2]) + ": P₂ = " + fmt(P2),
                            "P₁ = " + fmt(v[0]) + ", V₁ = " + fmt(v[1]));
                }
                case "charles" -> {
                    check(v, 3, "V1, T1(K), V2_or_T2(K)");
                    double ratio = v[0] / v[1];
                    double T2 = v[2] / ratio;
                    double V2 = ratio * v[2];
                    yield phResult("Charles's Law", "V₁/T₁ = V₂/T₂ (constant P, n)",
                            "If V₂ = " + fmt(v[2]) + ": T₂ = " + fmt(T2) + " K\n"
                                    + "If T₂ = " + fmt(v[2]) + " K: V₂ = " + fmt(V2),
                            "V₁ = " + fmt(v[0]) + ", T₁ = " + fmt(v[1]) + " K");
                }
                case "dalton" -> {
                    double total = 0;
                    for (double p : v) total += p;
                    yield phResult("Dalton's Law of Partial Pressures", "P_total = P₁ + P₂ + ...",
                            "P_total = " + fmt(total), "Partial pressures: " + formatArr(v));
                }
                case "graham" -> {
                    check(v, 2, "M1(g/mol), M2(g/mol)");
                    double ratio = Math.sqrt(v[1] / v[0]);
                    yield phResult("Graham's Law of Effusion", "rate₁/rate₂ = √(M₂/M₁)",
                            "rate₁/rate₂ = √(" + fmt(v[1]) + "/" + fmt(v[0]) + ") = " + fmt(ratio) + "\n"
                                    + "Gas 1 effuses " + fmt(ratio) + "× " + (ratio > 1 ? "faster" : "slower") + " than Gas 2",
                            "M₁ = " + fmt(v[0]) + " g/mol, M₂ = " + fmt(v[1]) + " g/mol");
                }
                default -> "Unknown formula. Use: ideal_gas, boyle, charles, gay_lussac, dalton, graham, density";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "chemistry_equilibrium", description = "Solve chemical equilibrium and kinetics problems: "
            + "equilibrium constant (Kc, Kp), reaction quotient (Q), "
            + "rate laws, half-life, and Arrhenius equation.")
    public String equilibrium(
            @ToolParam(description = "Formula: 'kc' (equilibrium constant from concentrations), "
                    + "'kp_from_kc' (Kp = Kc(RT)^Δn), 'half_life_first' (t½ = ln2/k), "
                    + "'half_life_zero' (t½ = [A]₀/2k), 'arrhenius' (k = Ae^(-Ea/RT)), "
                    + "'nernst' (E = E° - (RT/nF)lnQ)") String formula,
            @ToolParam(description = "Comma-separated values for the formula") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "kp_from_kc" -> {
                    check(v, 2, "Kc, Δn(gas moles products - reactants), T(K)");
                    double T = v.length > 2 ? v[2] : 298.15;
                    double Kp = v[0] * Math.pow(R_ATM * T, v[1]);
                    yield phResult("Kp from Kc", "Kp = Kc(RT)^Δn",
                            "Kp = " + sci(Kp),
                            "Kc = " + fmt(v[0]) + ", Δn = " + fmt(v[1]) + ", T = " + fmt(T) + " K");
                }
                case "half_life_first" -> {
                    check(v, 1, "rate constant k (s⁻¹)");
                    double t12 = Math.log(2) / v[0];
                    yield phResult("First-Order Half-Life", "t₁/₂ = ln(2)/k",
                            "t₁/₂ = " + sci(t12) + " s",
                            "k = " + sci(v[0]) + " s⁻¹");
                }
                case "half_life_zero" -> {
                    check(v, 2, "[A]₀ (mol/L), k (mol/L⋅s)");
                    double t12 = v[0] / (2 * v[1]);
                    yield phResult("Zero-Order Half-Life", "t₁/₂ = [A]₀/(2k)",
                            "t₁/₂ = " + fmt(t12) + " s",
                            "[A]₀ = " + fmt(v[0]) + " M, k = " + fmt(v[1]) + " M/s");
                }
                case "arrhenius" -> {
                    check(v, 3, "A (pre-exponential), Ea (J/mol), T (K)");
                    double k = v[0] * Math.exp(-v[1] / (R_GAS * v[2]));
                    yield phResult("Arrhenius Equation", "k = Ae^(-Eₐ/RT)",
                            "k = " + sci(k) + " (rate constant)",
                            "A = " + sci(v[0]) + ", Eₐ = " + fmt(v[1]) + " J/mol, T = " + fmt(v[2]) + " K");
                }
                case "nernst" -> {
                    check(v, 4, "E°(V), n(electrons), T(K), Q(reaction quotient)");
                    double E = v[0] - (R_GAS * v[2]) / (v[1] * FARADAY) * Math.log(v[3]);
                    yield phResult("Nernst Equation", "E = E° - (RT/nF)lnQ",
                            "E = " + fmt(E) + " V\n"
                                    + (E > 0 ? "Reaction is spontaneous (E > 0)" : "Reaction is non-spontaneous (E < 0)"),
                            "E° = " + fmt(v[0]) + " V, n = " + fmt(v[1]) + ", T = " + fmt(v[2]) + " K, Q = " + fmt(v[3]));
                }
                default -> "Unknown formula. Use: kp_from_kc, half_life_first, half_life_zero, arrhenius, nernst";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ── Formula Parser ──

    private Map<String, Double> parseFormula(String formula, double multiplier) {
        Map<String, Double> result = new LinkedHashMap<>();
        int i = 0;

        while (i < formula.length()) {
            char c = formula.charAt(i);

            if (c == '(') {
                // Find matching closing paren
                int depth = 1, j = i + 1;
                while (j < formula.length() && depth > 0) {
                    if (formula.charAt(j) == '(') depth++;
                    if (formula.charAt(j) == ')') depth--;
                    j++;
                }
                String inner = formula.substring(i + 1, j - 1);
                // Get subscript after )
                int subStart = j;
                while (j < formula.length() && Character.isDigit(formula.charAt(j))) j++;
                double sub = subStart < j ? Double.parseDouble(formula.substring(subStart, j)) : 1;

                Map<String, Double> innerResult = parseFormula(inner, multiplier * sub);
                innerResult.forEach((k, v) -> result.merge(k, v, Double::sum));
                i = j;

            } else if (Character.isUpperCase(c)) {
                // Element symbol
                int j = i + 1;
                while (j < formula.length() && Character.isLowerCase(formula.charAt(j))) j++;
                String element = formula.substring(i, j);

                // Get subscript
                int subStart = j;
                while (j < formula.length() && Character.isDigit(formula.charAt(j))) j++;
                double sub = subStart < j ? Double.parseDouble(formula.substring(subStart, j)) : 1;

                result.merge(element, sub * multiplier, Double::sum);
                i = j;

            } else {
                i++;
            }
        }

        return result;
    }

    // ── Helpers ──

    private double[] parseValues(String values) {
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i].strip());
        return result;
    }

    private void check(double[] v, int min, String names) {
        if (v.length < min) throw new IllegalArgumentException("Need " + min + " values: " + names);
    }

    private String phResult(String title, String formula, String answer, String given) {
        return title + "\n" + "─".repeat(title.length()) + "\n"
                + "Formula: " + formula + "\n"
                + "Given: " + given + "\n\n"
                + "Result:\n  " + answer.replace("\n", "\n  ");
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }

    private String sci(double v) { return String.format("%.4e", v); }

    private String formatArr(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(fmt(arr[i]));
        }
        return sb.append("]").toString();
    }

    private static final double R_GAS_STATIC = 8.314462618;
}
