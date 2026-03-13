package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MCP tool for university-level physics calculations.
 * Covers mechanics, thermodynamics, electromagnetism, waves, optics, and modern physics.
 */
@Service
public class PhysicsTool {

    private static final MathContext MC = new MathContext(12);

    // ── Physical Constants ──
    private static final double C = 299_792_458;           // speed of light (m/s)
    private static final double G = 6.67430e-11;           // gravitational constant (N⋅m²/kg²)
    private static final double H = 6.62607015e-34;        // Planck constant (J⋅s)
    private static final double H_BAR = H / (2 * Math.PI); // reduced Planck
    private static final double K_B = 1.380649e-23;        // Boltzmann constant (J/K)
    private static final double N_A = 6.02214076e23;       // Avogadro's number
    private static final double E_CHARGE = 1.602176634e-19; // elementary charge (C)
    private static final double EPSILON_0 = 8.8541878128e-12; // vacuum permittivity (F/m)
    private static final double MU_0 = 1.25663706212e-6;   // vacuum permeability (H/m)
    private static final double M_ELECTRON = 9.1093837015e-31; // electron mass (kg)
    private static final double M_PROTON = 1.67262192369e-27;  // proton mass (kg)
    private static final double SIGMA = 5.670374419e-8;    // Stefan–Boltzmann constant (W/m²⋅K⁴)
    private static final double R_GAS = 8.314462618;       // gas constant (J/mol⋅K)
    private static final double G_EARTH = 9.80665;         // standard gravity (m/s²)

    @Tool(name = "physics_constants", description = "Look up fundamental physical constants with their values, "
            + "units, and symbols. Covers mechanics, electromagnetism, thermodynamics, quantum, and relativity.")
    public String physicsConstants(
            @ToolParam(description = "Search term or category. Examples: 'speed of light', 'planck', 'all', "
                    + "'mechanics', 'electromagnetism', 'quantum', 'thermodynamics'") String query) {

        String q = query.strip().toLowerCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Physical Constants\n");
        sb.append("──────────────────\n\n");

        boolean all = "all".equals(q);

        if (all || q.contains("mechanic") || q.contains("gravit") || q.contains("speed") || q.contains("light")) {
            sb.append("MECHANICS & RELATIVITY\n");
            sb.append("  c   = ").append(sci(C)).append(" m/s          Speed of light in vacuum\n");
            sb.append("  G   = ").append(sci(G)).append(" N⋅m²/kg²    Gravitational constant\n");
            sb.append("  g   = ").append(G_EARTH).append(" m/s²                Standard gravity (Earth)\n\n");
        }
        if (all || q.contains("electro") || q.contains("charge") || q.contains("permit") || q.contains("coulomb")) {
            sb.append("ELECTROMAGNETISM\n");
            sb.append("  e   = ").append(sci(E_CHARGE)).append(" C           Elementary charge\n");
            sb.append("  ε₀  = ").append(sci(EPSILON_0)).append(" F/m         Vacuum permittivity\n");
            sb.append("  μ₀  = ").append(sci(MU_0)).append(" H/m         Vacuum permeability\n");
            sb.append("  k_e = ").append(sci(1 / (4 * Math.PI * EPSILON_0))).append(" N⋅m²/C²    Coulomb's constant\n\n");
        }
        if (all || q.contains("quantum") || q.contains("planck") || q.contains("electron") || q.contains("proton")) {
            sb.append("QUANTUM & ATOMIC\n");
            sb.append("  h   = ").append(sci(H)).append(" J⋅s          Planck constant\n");
            sb.append("  ℏ   = ").append(sci(H_BAR)).append(" J⋅s          Reduced Planck (h/2π)\n");
            sb.append("  mₑ  = ").append(sci(M_ELECTRON)).append(" kg          Electron mass\n");
            sb.append("  mₚ  = ").append(sci(M_PROTON)).append(" kg          Proton mass\n");
            sb.append("  Nₐ  = ").append(sci(N_A)).append(" mol⁻¹       Avogadro's number\n\n");
        }
        if (all || q.contains("thermo") || q.contains("boltzmann") || q.contains("stefan") || q.contains("gas")) {
            sb.append("THERMODYNAMICS\n");
            sb.append("  k_B = ").append(sci(K_B)).append(" J/K          Boltzmann constant\n");
            sb.append("  R   = ").append(R_GAS).append(" J/(mol⋅K)       Ideal gas constant\n");
            sb.append("  σ   = ").append(sci(SIGMA)).append(" W/(m²⋅K⁴)   Stefan–Boltzmann constant\n\n");
        }

        if (sb.toString().endsWith("──────────────────\n\n")) {
            sb.append("No constants matched '").append(query).append("'.\n");
            sb.append("Try: 'all', 'mechanics', 'electromagnetism', 'quantum', 'thermodynamics'");
        }

        return sb.toString();
    }

    @Tool(name = "physics_kinematics", description = "Solve 1D kinematics problems using the SUVAT equations. "
            + "Provide any 3 of the 5 variables (s, u, v, a, t) and the tool solves for the remaining 2. "
            + "s = displacement, u = initial velocity, v = final velocity, a = acceleration, t = time.")
    public String kinematics(
            @ToolParam(description = "Displacement in meters (or null if unknown)", required = false) Double s,
            @ToolParam(description = "Initial velocity in m/s (or null if unknown)", required = false) Double u,
            @ToolParam(description = "Final velocity in m/s (or null if unknown)", required = false) Double v,
            @ToolParam(description = "Acceleration in m/s² (or null if unknown)", required = false) Double a,
            @ToolParam(description = "Time in seconds (or null if unknown)", required = false) Double t) {

        int known = (s != null ? 1 : 0) + (u != null ? 1 : 0) + (v != null ? 1 : 0) + (a != null ? 1 : 0) + (t != null ? 1 : 0);
        if (known < 3) return "Error: Need at least 3 known variables. Provide s, u, v, a, t.";

        StringBuilder sb = new StringBuilder();
        sb.append("Kinematics (SUVAT) Solution\n");
        sb.append("──────────────────────────\n\n");
        sb.append("GIVEN:\n");
        if (s != null) sb.append("  s = ").append(fmt(s)).append(" m\n");
        if (u != null) sb.append("  u = ").append(fmt(u)).append(" m/s\n");
        if (v != null) sb.append("  v = ").append(fmt(v)).append(" m/s\n");
        if (a != null) sb.append("  a = ").append(fmt(a)).append(" m/s²\n");
        if (t != null) sb.append("  t = ").append(fmt(t)).append(" s\n");

        // Solve for unknowns
        try {
            // v = u + at
            if (v == null && u != null && a != null && t != null) v = u + a * t;
            if (u == null && v != null && a != null && t != null) u = v - a * t;
            if (a == null && u != null && v != null && t != null) a = (v - u) / t;
            if (t == null && u != null && v != null && a != null && a != 0) t = (v - u) / a;

            // s = ut + ½at²
            if (s == null && u != null && t != null && a != null) s = u * t + 0.5 * a * t * t;
            if (u == null && s != null && t != null && a != null && t != 0) u = (s - 0.5 * a * t * t) / t;

            // v² = u² + 2as
            if (v == null && u != null && a != null && s != null) {
                double v2 = u * u + 2 * a * s;
                v = v2 >= 0 ? Math.sqrt(v2) : -Math.sqrt(-v2);
            }
            if (s == null && u != null && v != null && a != null && a != 0) s = (v * v - u * u) / (2 * a);

            // s = ½(u + v)t
            if (t == null && u != null && v != null && s != null && (u + v) != 0) t = 2 * s / (u + v);
            if (s == null && u != null && v != null && t != null) s = 0.5 * (u + v) * t;

            // Final pass for remaining unknowns
            if (a == null && u != null && v != null && t != null) a = (v - u) / t;
            if (t == null && u != null && a != null && s != null) {
                // s = ut + ½at² → ½at² + ut - s = 0
                double disc = u * u + 2 * a * s;
                if (disc >= 0) t = (-u + Math.sqrt(disc)) / a;
            }

            sb.append("\nSOLVED:\n");
            sb.append("  s = ").append(s != null ? fmt(s) + " m" : "unknown").append("\n");
            sb.append("  u = ").append(u != null ? fmt(u) + " m/s" : "unknown").append("\n");
            sb.append("  v = ").append(v != null ? fmt(v) + " m/s" : "unknown").append("\n");
            sb.append("  a = ").append(a != null ? fmt(a) + " m/s²" : "unknown").append("\n");
            sb.append("  t = ").append(t != null ? fmt(t) + " s" : "unknown").append("\n");

            sb.append("\nEQUATIONS USED:\n");
            sb.append("  v = u + at\n");
            sb.append("  s = ut + ½at²\n");
            sb.append("  v² = u² + 2as\n");
            sb.append("  s = ½(u + v)t\n");

        } catch (Exception e) {
            sb.append("\nError solving: ").append(e.getMessage());
        }

        return sb.toString();
    }

    @Tool(name = "physics_forces", description = "Calculate forces and related quantities: "
            + "Newton's laws, gravity, friction, centripetal force, spring force, drag, "
            + "work, kinetic/potential energy, momentum, and impulse.")
    public String forces(
            @ToolParam(description = "Formula to use: 'newton' (F=ma), 'gravity' (F=GMm/r²), "
                    + "'weight' (W=mg), 'friction' (f=μN), 'centripetal' (F=mv²/r), "
                    + "'spring' (F=-kx), 'work' (W=Fd cosθ), 'kinetic_energy' (KE=½mv²), "
                    + "'potential_energy' (PE=mgh), 'momentum' (p=mv), 'impulse' (J=FΔt), "
                    + "'power' (P=W/t or P=Fv)") String formula,
            @ToolParam(description = "Comma-separated values for the chosen formula:\n"
                    + "  newton: mass(kg), acceleration(m/s²)\n"
                    + "  gravity: mass1(kg), mass2(kg), distance(m)\n"
                    + "  weight: mass(kg) [optional: g in m/s², default 9.81]\n"
                    + "  friction: coefficient(μ), normal_force(N)\n"
                    + "  centripetal: mass(kg), velocity(m/s), radius(m)\n"
                    + "  spring: spring_constant(N/m), displacement(m)\n"
                    + "  work: force(N), distance(m), angle(degrees)\n"
                    + "  kinetic_energy: mass(kg), velocity(m/s)\n"
                    + "  potential_energy: mass(kg), height(m) [optional: g]\n"
                    + "  momentum: mass(kg), velocity(m/s)\n"
                    + "  impulse: force(N), time(s)\n"
                    + "  power: work_or_force, time_or_velocity") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "newton" -> {
                    check(v, 2, "mass, acceleration");
                    double F = v[0] * v[1];
                    yield result("Newton's Second Law", "F = ma",
                            "F = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(F) + " N",
                            "m = " + fmt(v[0]) + " kg, a = " + fmt(v[1]) + " m/s²");
                }
                case "gravity" -> {
                    check(v, 3, "M, m, r");
                    double F = G * v[0] * v[1] / (v[2] * v[2]);
                    yield result("Newton's Law of Gravitation", "F = GMm/r²",
                            "F = " + sci(F) + " N",
                            "M = " + sci(v[0]) + " kg, m = " + sci(v[1]) + " kg, r = " + fmt(v[2]) + " m");
                }
                case "weight" -> {
                    double g = v.length > 1 ? v[1] : G_EARTH;
                    double W = v[0] * g;
                    yield result("Weight", "W = mg",
                            "W = " + fmt(v[0]) + " × " + fmt(g) + " = " + fmt(W) + " N",
                            "m = " + fmt(v[0]) + " kg, g = " + fmt(g) + " m/s²");
                }
                case "friction" -> {
                    check(v, 2, "μ, N");
                    double f = v[0] * v[1];
                    yield result("Friction Force", "f = μN",
                            "f = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(f) + " N",
                            "μ = " + fmt(v[0]) + ", N = " + fmt(v[1]) + " N");
                }
                case "centripetal" -> {
                    check(v, 3, "mass, velocity, radius");
                    double F = v[0] * v[1] * v[1] / v[2];
                    double a = v[1] * v[1] / v[2];
                    yield result("Centripetal Force", "F = mv²/r",
                            "F = " + fmt(F) + " N\nCentripetal acceleration a = v²/r = " + fmt(a) + " m/s²",
                            "m = " + fmt(v[0]) + " kg, v = " + fmt(v[1]) + " m/s, r = " + fmt(v[2]) + " m");
                }
                case "spring" -> {
                    check(v, 2, "k, x");
                    double F = -v[0] * v[1];
                    double PE = 0.5 * v[0] * v[1] * v[1];
                    yield result("Hooke's Law (Spring)", "F = -kx",
                            "F = " + fmt(F) + " N\nSpring PE = ½kx² = " + fmt(PE) + " J",
                            "k = " + fmt(v[0]) + " N/m, x = " + fmt(v[1]) + " m");
                }
                case "work" -> {
                    check(v, 3, "force, distance, angle");
                    double W = v[0] * v[1] * Math.cos(Math.toRadians(v[2]));
                    yield result("Work", "W = Fd cos(θ)",
                            "W = " + fmt(v[0]) + " × " + fmt(v[1]) + " × cos(" + fmt(v[2]) + "°) = " + fmt(W) + " J",
                            "F = " + fmt(v[0]) + " N, d = " + fmt(v[1]) + " m, θ = " + fmt(v[2]) + "°");
                }
                case "kinetic_energy", "ke" -> {
                    check(v, 2, "mass, velocity");
                    double KE = 0.5 * v[0] * v[1] * v[1];
                    yield result("Kinetic Energy", "KE = ½mv²",
                            "KE = 0.5 × " + fmt(v[0]) + " × " + fmt(v[1]) + "² = " + fmt(KE) + " J",
                            "m = " + fmt(v[0]) + " kg, v = " + fmt(v[1]) + " m/s");
                }
                case "potential_energy", "pe" -> {
                    double g = v.length > 2 ? v[2] : G_EARTH;
                    check(v, 2, "mass, height");
                    double PE = v[0] * g * v[1];
                    yield result("Gravitational Potential Energy", "PE = mgh",
                            "PE = " + fmt(v[0]) + " × " + fmt(g) + " × " + fmt(v[1]) + " = " + fmt(PE) + " J",
                            "m = " + fmt(v[0]) + " kg, h = " + fmt(v[1]) + " m, g = " + fmt(g) + " m/s²");
                }
                case "momentum" -> {
                    check(v, 2, "mass, velocity");
                    double p = v[0] * v[1];
                    yield result("Momentum", "p = mv",
                            "p = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(p) + " kg⋅m/s",
                            "m = " + fmt(v[0]) + " kg, v = " + fmt(v[1]) + " m/s");
                }
                case "impulse" -> {
                    check(v, 2, "force, time");
                    double J = v[0] * v[1];
                    yield result("Impulse", "J = FΔt",
                            "J = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(J) + " N⋅s",
                            "F = " + fmt(v[0]) + " N, Δt = " + fmt(v[1]) + " s");
                }
                case "power" -> {
                    check(v, 2, "work/force, time/velocity");
                    double P = v[0] * v[1]; // P = W/t or P = Fv depending on context
                    yield result("Power", "P = W/t  or  P = Fv",
                            "P = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(P) + " W",
                            "Input values: " + fmt(v[0]) + ", " + fmt(v[1]));
                }
                default -> "Unknown formula. Use: newton, gravity, weight, friction, centripetal, spring, "
                        + "work, kinetic_energy, potential_energy, momentum, impulse, power";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "physics_waves", description = "Calculate wave properties: frequency, wavelength, period, "
            + "wave speed, photon energy, de Broglie wavelength, Doppler effect, "
            + "and standing wave harmonics.")
    public String waves(
            @ToolParam(description = "Formula: 'wave_speed' (v=fλ), 'photon_energy' (E=hf), "
                    + "'de_broglie' (λ=h/mv), 'doppler' (f'=f(v±v_o)/(v∓v_s)), "
                    + "'standing_wave' (harmonic frequencies), 'snells_law' (n₁sinθ₁=n₂sinθ₂), "
                    + "'thin_lens' (1/f=1/do+1/di)") String formula,
            @ToolParam(description = "Comma-separated values (see formula descriptions in tool name)") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "wave_speed" -> {
                    check(v, 2, "frequency(Hz), wavelength(m)");
                    double speed = v[0] * v[1];
                    double period = 1.0 / v[0];
                    yield result("Wave Speed", "v = fλ",
                            "v = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(speed) + " m/s\nPeriod T = 1/f = " + sci(period) + " s",
                            "f = " + fmt(v[0]) + " Hz, λ = " + fmt(v[1]) + " m");
                }
                case "photon_energy" -> {
                    // input: frequency OR wavelength (if < 1, treat as wavelength in meters)
                    double freq, wavelength, energy;
                    if (v[0] > 1e6) { // likely frequency in Hz
                        freq = v[0];
                        wavelength = C / freq;
                    } else { // likely wavelength in meters
                        wavelength = v[0];
                        freq = C / wavelength;
                    }
                    energy = H * freq;
                    double eV = energy / E_CHARGE;
                    yield result("Photon Energy", "E = hf = hc/λ",
                            "E = " + sci(energy) + " J = " + fmt(eV) + " eV\n"
                                    + "f = " + sci(freq) + " Hz\nλ = " + sci(wavelength) + " m",
                            "Input: " + sci(v[0]));
                }
                case "de_broglie" -> {
                    check(v, 2, "mass(kg), velocity(m/s)");
                    double lambda = H / (v[0] * v[1]);
                    yield result("de Broglie Wavelength", "λ = h/(mv)",
                            "λ = " + sci(lambda) + " m",
                            "m = " + sci(v[0]) + " kg, v = " + fmt(v[1]) + " m/s");
                }
                case "doppler" -> {
                    check(v, 4, "source_freq(Hz), wave_speed(m/s), observer_speed(m/s), source_speed(m/s)");
                    // Observer approaching, source approaching
                    double fApproach = v[0] * (v[1] + v[2]) / (v[1] - v[3]);
                    double fRecede = v[0] * (v[1] - v[2]) / (v[1] + v[3]);
                    yield result("Doppler Effect", "f' = f(v ± v_o)/(v ∓ v_s)",
                            "Approaching: f' = " + fmt(fApproach) + " Hz\n"
                                    + "Receding: f' = " + fmt(fRecede) + " Hz",
                            "f₀ = " + fmt(v[0]) + " Hz, v = " + fmt(v[1]) + " m/s, v_o = " + fmt(v[2]) + " m/s, v_s = " + fmt(v[3]) + " m/s");
                }
                case "snells_law", "snell" -> {
                    check(v, 3, "n1, n2, angle1(degrees)");
                    double sinTheta2 = v[0] * Math.sin(Math.toRadians(v[2])) / v[1];
                    if (Math.abs(sinTheta2) > 1) {
                        double criticalAngle = Math.toDegrees(Math.asin(v[1] / v[0]));
                        yield result("Snell's Law — Total Internal Reflection", "n₁sinθ₁ = n₂sinθ₂",
                                "Total internal reflection occurs (sinθ₂ > 1)\nCritical angle = " + fmt(criticalAngle) + "°",
                                "n₁ = " + fmt(v[0]) + ", n₂ = " + fmt(v[1]) + ", θ₁ = " + fmt(v[2]) + "°");
                    }
                    double theta2 = Math.toDegrees(Math.asin(sinTheta2));
                    yield result("Snell's Law", "n₁sinθ₁ = n₂sinθ₂",
                            "θ₂ = " + fmt(theta2) + "°",
                            "n₁ = " + fmt(v[0]) + ", n₂ = " + fmt(v[1]) + ", θ₁ = " + fmt(v[2]) + "°");
                }
                case "thin_lens" -> {
                    check(v, 2, "focal_length(m), object_distance(m)");
                    double di = 1.0 / (1.0 / v[0] - 1.0 / v[1]);
                    double magnification = -di / v[1];
                    String imageType = di > 0 ? "Real, " : "Virtual, ";
                    imageType += Math.abs(magnification) > 1 ? "Enlarged" : "Reduced";
                    imageType += magnification > 0 ? ", Upright" : ", Inverted";
                    yield result("Thin Lens Equation", "1/f = 1/dₒ + 1/dᵢ",
                            "Image distance dᵢ = " + fmt(di) + " m\nMagnification M = " + fmt(magnification)
                                    + "\nImage: " + imageType,
                            "f = " + fmt(v[0]) + " m, dₒ = " + fmt(v[1]) + " m");
                }
                default -> "Unknown formula. Use: wave_speed, photon_energy, de_broglie, doppler, snells_law, thin_lens";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "physics_electricity", description = "Calculate electrical quantities: Ohm's law, power, "
            + "resistors in series/parallel, capacitors, Coulomb's law, electric field, "
            + "RC/RL circuit time constants, and electromagnetic induction.")
    public String electricity(
            @ToolParam(description = "Formula: 'ohm' (V=IR), 'power' (P=IV), 'resistors_series', "
                    + "'resistors_parallel', 'capacitance' (C=Q/V), 'capacitors_series', "
                    + "'capacitors_parallel', 'coulomb' (F=kQq/r²), 'electric_field' (E=kQ/r²), "
                    + "'rc_circuit' (τ=RC), 'rl_circuit' (τ=L/R), 'energy_capacitor' (U=½CV²)") String formula,
            @ToolParam(description = "Comma-separated values for the chosen formula") String values) {

        try {
            double[] v = parseValues(values);
            double k_e = 1.0 / (4 * Math.PI * EPSILON_0);

            return switch (formula.strip().toLowerCase()) {
                case "ohm" -> {
                    check(v, 2, "two of: V(volts), I(amps), R(ohms) — solver finds the third");
                    // Assume first two are what's given; figure out what to solve
                    // Convention: provide the two knowns. We'll compute all combos.
                    double val1 = v[0], val2 = v[1];
                    yield result("Ohm's Law", "V = IR",
                            "If V=" + fmt(val1) + ", I=" + fmt(val2) + ": R = " + fmt(val1 / val2) + " Ω\n"
                                    + "If V=" + fmt(val1) + ", R=" + fmt(val2) + ": I = " + fmt(val1 / val2) + " A\n"
                                    + "If I=" + fmt(val1) + ", R=" + fmt(val2) + ": V = " + fmt(val1 * val2) + " V",
                            "Values: " + fmt(val1) + ", " + fmt(val2));
                }
                case "power" -> {
                    check(v, 2, "current(A), voltage(V)");
                    double P = v[0] * v[1];
                    yield result("Electrical Power", "P = IV",
                            "P = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(P) + " W",
                            "I = " + fmt(v[0]) + " A, V = " + fmt(v[1]) + " V");
                }
                case "resistors_series" -> {
                    double total = 0;
                    for (double r : v) total += r;
                    yield result("Resistors in Series", "R_total = R₁ + R₂ + ...",
                            "R_total = " + fmt(total) + " Ω", "Resistors: " + formatArr(v) + " Ω");
                }
                case "resistors_parallel" -> {
                    double invTotal = 0;
                    for (double r : v) invTotal += 1.0 / r;
                    yield result("Resistors in Parallel", "1/R_total = 1/R₁ + 1/R₂ + ...",
                            "R_total = " + fmt(1.0 / invTotal) + " Ω", "Resistors: " + formatArr(v) + " Ω");
                }
                case "coulomb" -> {
                    check(v, 3, "Q1(C), Q2(C), r(m)");
                    double F = k_e * v[0] * v[1] / (v[2] * v[2]);
                    yield result("Coulomb's Law", "F = kQ₁Q₂/r²",
                            "F = " + sci(F) + " N (" + (F > 0 ? "repulsive" : "attractive") + ")",
                            "Q₁ = " + sci(v[0]) + " C, Q₂ = " + sci(v[1]) + " C, r = " + fmt(v[2]) + " m");
                }
                case "electric_field" -> {
                    check(v, 2, "charge(C), distance(m)");
                    double E = k_e * v[0] / (v[1] * v[1]);
                    yield result("Electric Field", "E = kQ/r²",
                            "E = " + sci(E) + " N/C", "Q = " + sci(v[0]) + " C, r = " + fmt(v[1]) + " m");
                }
                case "capacitance" -> {
                    check(v, 2, "charge(C), voltage(V)");
                    double cap = v[0] / v[1];
                    yield result("Capacitance", "C = Q/V",
                            "C = " + sci(cap) + " F", "Q = " + sci(v[0]) + " C, V = " + fmt(v[1]) + " V");
                }
                case "capacitors_series" -> {
                    double invTotal = 0;
                    for (double c : v) invTotal += 1.0 / c;
                    yield result("Capacitors in Series", "1/C_total = 1/C₁ + 1/C₂ + ...",
                            "C_total = " + sci(1.0 / invTotal) + " F", "Capacitors: " + formatArr(v) + " F");
                }
                case "capacitors_parallel" -> {
                    double total = 0;
                    for (double c : v) total += c;
                    yield result("Capacitors in Parallel", "C_total = C₁ + C₂ + ...",
                            "C_total = " + sci(total) + " F", "Capacitors: " + formatArr(v) + " F");
                }
                case "rc_circuit" -> {
                    check(v, 2, "R(Ω), C(F)");
                    double tau = v[0] * v[1];
                    yield result("RC Circuit Time Constant", "τ = RC",
                            "τ = " + sci(tau) + " s\n63.2% charge at t=τ, 99.3% at t=5τ (" + sci(5 * tau) + " s)",
                            "R = " + fmt(v[0]) + " Ω, C = " + sci(v[1]) + " F");
                }
                case "rl_circuit" -> {
                    check(v, 2, "L(H), R(Ω)");
                    double tau = v[0] / v[1];
                    yield result("RL Circuit Time Constant", "τ = L/R",
                            "τ = " + sci(tau) + " s", "L = " + fmt(v[0]) + " H, R = " + fmt(v[1]) + " Ω");
                }
                case "energy_capacitor" -> {
                    check(v, 2, "C(F), V(volts)");
                    double U = 0.5 * v[0] * v[1] * v[1];
                    yield result("Energy Stored in Capacitor", "U = ½CV²",
                            "U = " + sci(U) + " J", "C = " + sci(v[0]) + " F, V = " + fmt(v[1]) + " V");
                }
                default -> "Unknown formula. Use: ohm, power, resistors_series, resistors_parallel, "
                        + "coulomb, electric_field, capacitance, capacitors_series, capacitors_parallel, "
                        + "rc_circuit, rl_circuit, energy_capacitor";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "physics_thermodynamics", description = "Solve thermodynamics problems: ideal gas law, "
            + "heat transfer, thermal expansion, entropy, Carnot efficiency, "
            + "Stefan-Boltzmann radiation, and specific heat.")
    public String thermodynamics(
            @ToolParam(description = "Formula: 'ideal_gas' (PV=nRT), 'heat_transfer' (Q=mcΔT), "
                    + "'thermal_expansion' (ΔL=αLΔT), 'carnot' (η=1-Tc/Th), "
                    + "'stefan_boltzmann' (P=σεAT⁴), 'entropy' (ΔS=Q/T), "
                    + "'latent_heat' (Q=mL)") String formula,
            @ToolParam(description = "Comma-separated values for the formula") String values) {

        try {
            double[] v = parseValues(values);

            return switch (formula.strip().toLowerCase()) {
                case "ideal_gas" -> {
                    check(v, 3, "three of: P(Pa), V(m³), n(mol), T(K) — solves for 4th");
                    // Assume P, V, T given → solve for n, or P, V, n → solve for T, etc.
                    // Let user provide 3 values, figure it out by count
                    if (v.length == 3) {
                        // PV = nRT → various combos
                        double P = v[0], V2 = v[1], T = v[2];
                        double n = P * V2 / (R_GAS * T);
                        yield result("Ideal Gas Law", "PV = nRT",
                                "n = PV/(RT) = " + fmt(n) + " mol\n"
                                        + "Also: T = PV/(nR) = " + fmt(P * V2 / (n * R_GAS)) + " K",
                                "P = " + sci(P) + " Pa, V = " + sci(V2) + " m³, T = " + fmt(T) + " K\nR = " + R_GAS + " J/(mol⋅K)");
                    }
                    yield "Provide 3 values: P(Pa), V(m³), and T(K) to solve for n";
                }
                case "heat_transfer" -> {
                    check(v, 3, "mass(kg), specific_heat(J/kg⋅K), ΔT(K)");
                    double Q = v[0] * v[1] * v[2];
                    yield result("Heat Transfer", "Q = mcΔT",
                            "Q = " + fmt(v[0]) + " × " + fmt(v[1]) + " × " + fmt(v[2]) + " = " + fmt(Q) + " J",
                            "m = " + fmt(v[0]) + " kg, c = " + fmt(v[1]) + " J/(kg⋅K), ΔT = " + fmt(v[2]) + " K");
                }
                case "thermal_expansion" -> {
                    check(v, 3, "α(coefficient /K), L(initial length m), ΔT(K)");
                    double dL = v[0] * v[1] * v[2];
                    double Lnew = v[1] + dL;
                    yield result("Linear Thermal Expansion", "ΔL = αL₀ΔT",
                            "ΔL = " + sci(dL) + " m\nNew length = " + fmt(Lnew) + " m",
                            "α = " + sci(v[0]) + " /K, L₀ = " + fmt(v[1]) + " m, ΔT = " + fmt(v[2]) + " K");
                }
                case "carnot" -> {
                    check(v, 2, "T_cold(K), T_hot(K)");
                    double eff = 1 - v[0] / v[1];
                    yield result("Carnot Efficiency", "η = 1 - Tc/Th",
                            "η = " + fmt(eff * 100) + "%",
                            "T_cold = " + fmt(v[0]) + " K, T_hot = " + fmt(v[1]) + " K");
                }
                case "stefan_boltzmann" -> {
                    check(v, 3, "emissivity(0-1), area(m²), temperature(K)");
                    double P = SIGMA * v[0] * v[1] * Math.pow(v[2], 4);
                    yield result("Stefan–Boltzmann Radiation", "P = σεAT⁴",
                            "P = " + sci(P) + " W",
                            "ε = " + fmt(v[0]) + ", A = " + fmt(v[1]) + " m², T = " + fmt(v[2]) + " K");
                }
                case "entropy" -> {
                    check(v, 2, "heat(J), temperature(K)");
                    double dS = v[0] / v[1];
                    yield result("Entropy Change", "ΔS = Q/T",
                            "ΔS = " + fmt(v[0]) + " / " + fmt(v[1]) + " = " + fmt(dS) + " J/K",
                            "Q = " + fmt(v[0]) + " J, T = " + fmt(v[1]) + " K");
                }
                case "latent_heat" -> {
                    check(v, 2, "mass(kg), latent_heat(J/kg)");
                    double Q = v[0] * v[1];
                    yield result("Latent Heat", "Q = mL",
                            "Q = " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(Q) + " J",
                            "m = " + fmt(v[0]) + " kg, L = " + fmt(v[1]) + " J/kg");
                }
                default -> "Unknown formula. Use: ideal_gas, heat_transfer, thermal_expansion, "
                        + "carnot, stefan_boltzmann, entropy, latent_heat";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
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

    private String result(String title, String formula, String answer, String given) {
        return title + "\n" + "─".repeat(title.length()) + "\n"
                + "Formula: " + formula + "\n"
                + "Given: " + given + "\n\n"
                + "Result:\n  " + answer.replace("\n", "\n  ");
    }

    private String fmt(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }

    private String sci(double v) {
        return String.format("%.6e", v);
    }

    private String formatArr(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(fmt(arr[i]));
        }
        return sb.append("]").toString();
    }
}
