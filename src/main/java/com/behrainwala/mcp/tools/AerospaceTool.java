package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MCP tools for advanced aerospace engineering calculations.
 * Covers oblique shocks, Prandtl-Meyer expansion, rocket propulsion,
 * flight performance, aerodynamic heating, and turbomachinery.
 */
@Service
public class AerospaceTool {

    private static final MathContext MC      = new MathContext(10);
    private static final double G0           = 9.80665;         // standard gravity (m/s²)
    private static final double GAMMA_AIR    = 1.4;
    private static final double CP_AIR       = 1004.5;          // J/(kg·K)
    private static final double SIGMA        = 5.670374419e-8;  // Stefan-Boltzmann (W/m²·K⁴)
    private static final double PR_AIR       = 0.72;            // Prandtl number for air

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Oblique Shock
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_oblique_shock",
          description = "Oblique shock wave relations for supersonic flow over a wedge or compression ramp. "
                  + "Solves the θ-β-M relation: given M₁ > 1 and either the flow deflection angle θ "
                  + "(wedge half-angle) or the shock wave angle β, computes the other angle and "
                  + "downstream Mach M₂, pressure/temperature/density ratios, and stagnation pressure loss. "
                  + "Always returns the weak-shock (attached) solution. "
                  + "Use fluid_normal_shock for a normal shock (β = 90°). "
                  + "Use for: supersonic inlets, compression ramps, waveriders, oblique shock trains.")
    public String obliqueShock(
            @ToolParam(description = "Upstream Mach number M₁ (must be > 1)") Double M1,
            @ToolParam(description = "Flow deflection angle θ in degrees (wedge half-angle / turning angle). "
                    + "Provide this OR shockAngle — not both.", required = false) Double deflectionAngleDeg,
            @ToolParam(description = "Shock wave angle β in degrees (angle of shock to freestream). "
                    + "Provide this OR deflectionAngle — not both.", required = false) Double shockAngleDeg,
            @ToolParam(description = "Upstream static pressure P₁ in Pa (optional; enables absolute P₂)", required = false) Double P1,
            @ToolParam(description = "Upstream static temperature T₁ in K (optional; enables absolute T₂)", required = false) Double T1,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (M1 == null) return "Error: M1 is required.";
        if (M1 <= 1.0) return "Error: M1 must be > 1. Got M1 = " + fmt(M1) + ".";
        if (deflectionAngleDeg == null && shockAngleDeg == null)
            return "Error: Provide either deflectionAngle (θ) or shockAngle (β), not neither.";

        double g     = (gamma != null) ? gamma : GAMMA_AIR;
        double muDeg = Math.toDegrees(Math.asin(1.0 / M1));

        double theta, beta;

        if (shockAngleDeg != null) {
            beta = shockAngleDeg;
            if (beta <= muDeg || beta > 90.0)
                return "Error: shockAngle β = " + fmt(beta) + "° must be between the Mach angle μ = "
                        + fmt(muDeg) + "° and 90°.";
            theta = Math.toDegrees(thetaFromBetaRad(M1, Math.toRadians(beta), g));
        } else {
            theta = deflectionAngleDeg;
            if (theta <= 0) return "Error: deflectionAngle θ must be positive.";
            double betaMaxRad = findBetaAtMaxTheta(M1, g);
            double thetaMax   = Math.toDegrees(thetaFromBetaRad(M1, betaMaxRad, g));
            if (theta >= thetaMax)
                return String.format("Error: deflectionAngle %.2f° exceeds the maximum attached-shock "
                        + "deflection %.2f° for M₁ = %.2f — shock detaches.", theta, thetaMax, M1);
            beta = Math.toDegrees(findWeakBeta(M1, Math.toRadians(theta), g));
        }

        double betaRad  = Math.toRadians(beta);
        double thetaRad = Math.toRadians(theta);
        double Mn1      = M1 * Math.sin(betaRad);
        double g1 = g - 1, g2 = g + 1;
        double Mn1sq    = Mn1 * Mn1;

        double Mn2sq   = (g1 * Mn1sq + 2) / (2 * g * Mn1sq - g1);
        double Mn2     = Math.sqrt(Mn2sq);
        double M2      = Mn2 / Math.sin(betaRad - thetaRad);
        double Pratio  = (2 * g * Mn1sq - g1) / g2;
        double rhoRatio = g2 * Mn1sq / (g1 * Mn1sq + 2);
        double Tratio  = Pratio / rhoRatio;
        double P0ratio = Math.pow(g2 * Mn1sq / (g1 * Mn1sq + 2), g / g1)
                       * Math.pow(g2 / (2 * g * Mn1sq - g1), 1.0 / g1);

        StringBuilder sb = new StringBuilder();
        sb.append("Oblique Shock Wave\n──────────────────\n\n");
        sb.append("GEOMETRY:\n");
        sb.append("  M₁ = ").append(fmt(M1)).append("  (upstream, supersonic)\n");
        sb.append("  μ  = ").append(fmt(muDeg)).append("°  (Mach angle = arcsin(1/M₁))\n");
        sb.append("  θ  = ").append(fmt(theta)).append("°  (flow deflection / wedge half-angle)\n");
        sb.append("  β  = ").append(fmt(beta)).append("°  (shock wave angle to freestream)\n");
        sb.append("  γ  = ").append(fmt(g)).append("\n\n");
        sb.append("  Mn₁ = M₁·sin(β) = ").append(fmt(Mn1)).append("  (normal component upstream)\n\n");
        sb.append("DOWNSTREAM:\n");
        sb.append("  M₂      = Mn₂/sin(β−θ) = ").append(fmt(M2)).append("\n");
        sb.append("  Mn₂     = ").append(fmt(Mn2)).append("  (normal component downstream)\n");
        sb.append("  P₂/P₁   = ").append(fmt(Pratio));
        if (P1 != null) sb.append("  → P₂ = ").append(fmt(P1 * Pratio)).append(" Pa");
        sb.append("\n  T₂/T₁   = ").append(fmt(Tratio));
        if (T1 != null) sb.append("  → T₂ = ").append(fmt(T1 * Tratio)).append(" K");
        sb.append("\n  ρ₂/ρ₁   = ").append(fmt(rhoRatio)).append("\n");
        sb.append("  P₀₂/P₀₁ = ").append(fmt(P0ratio))
          .append("  (").append(fmt((1 - P0ratio) * 100)).append("% stagnation pressure lost)\n\n");
        sb.append("Formula: tan(θ) = 2cot(β)·(M₁²sin²β−1)/(M₁²(γ+cos2β)+2)");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Prandtl-Meyer Expansion
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_prandtl_meyer",
          description = "Prandtl-Meyer expansion fan for isentropic supersonic flow around a convex corner. "
                  + "Given upstream Mach M₁ ≥ 1 and expansion (turning) angle Δν, computes downstream "
                  + "Mach M₂ and pressure/temperature/density ratios. Stagnation conditions are preserved "
                  + "(isentropic — unlike shocks). "
                  + "Also computes ν(M₁), ν(M₂), and Mach angles. "
                  + "Use for: nozzle diverging section design, waverider lower surface, method of characteristics.")
    public String prandtlMeyer(
            @ToolParam(description = "Upstream Mach number M₁ (must be ≥ 1; use M₁ = 1 for sonic entry)") Double M1,
            @ToolParam(description = "Expansion (turning) angle in degrees (positive, convex corner). "
                    + "Maximum for γ=1.4: ≈ 130.45°.") Double turningAngleDeg,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (M1 == null || turningAngleDeg == null) return "Error: M1 and turningAngle are required.";
        if (M1 < 1.0) return "Error: M1 must be ≥ 1. Got M1 = " + fmt(M1) + ".";
        if (turningAngleDeg <= 0) return "Error: turningAngle must be positive (convex/expansion corner).";

        double g      = (gamma != null) ? gamma : GAMMA_AIR;
        double nuMax  = Math.PI / 2.0 * (Math.sqrt((g + 1) / (g - 1)) - 1);
        double nu1    = pmFunction(M1, g);
        double nu2    = nu1 + Math.toRadians(turningAngleDeg);

        if (nu2 > nuMax)
            return String.format("Error: turningAngle %.2f° causes ν₂ = %.4f rad, exceeding ν_max = %.4f rad (%.2f°). "
                    + "Reduce the turning angle.", turningAngleDeg, nu2, nuMax, Math.toDegrees(nuMax));

        double M2      = mFromPM(nu2, g);
        double mu1Deg  = Math.toDegrees(Math.asin(1.0 / M1));
        double mu2Deg  = Math.toDegrees(Math.asin(1.0 / M2));

        double f1      = 1.0 + (g - 1) / 2.0 * M1 * M1;
        double f2      = 1.0 + (g - 1) / 2.0 * M2 * M2;
        double Tratio  = f1 / f2;
        double Pratio  = Math.pow(f1 / f2, g / (g - 1));
        double rhoRatio = Math.pow(f1 / f2, 1.0 / (g - 1));

        StringBuilder sb = new StringBuilder();
        sb.append("Prandtl-Meyer Expansion Fan\n───────────────────────────\n\n");
        sb.append("GIVEN:\n");
        sb.append("  M₁ = ").append(fmt(M1)).append(",  Δν = ").append(fmt(turningAngleDeg))
          .append("°,  γ = ").append(fmt(g)).append("\n\n");
        sb.append("PRANDTL-MEYER FUNCTION  ν(M) = √((γ+1)/(γ-1))·arctan(√((γ-1)/(γ+1)·(M²-1))) − arctan(√(M²-1)):\n");
        sb.append("  ν(M₁) = ").append(fmt(Math.toDegrees(nu1))).append("°\n");
        sb.append("  ν(M₂) = ν(M₁) + Δν = ").append(fmt(Math.toDegrees(nu2))).append("°\n");
        sb.append("  ν_max  = ").append(fmt(Math.toDegrees(nuMax))).append("°  (limit as M → ∞)\n\n");
        sb.append("DOWNSTREAM:\n");
        sb.append("  M₂ = ").append(fmt(M2)).append("  (solved: ν(M₂) = ν₂)\n");
        sb.append("  μ₁ = arcsin(1/M₁) = ").append(fmt(mu1Deg)).append("°  (upstream Mach angle)\n");
        sb.append("  μ₂ = arcsin(1/M₂) = ").append(fmt(mu2Deg)).append("°  (downstream Mach angle)\n\n");
        sb.append("ISENTROPIC RATIOS (P₀, T₀ conserved — no entropy change):\n");
        sb.append("  T₂/T₁ = ").append(fmt(Tratio)).append("\n");
        sb.append("  P₂/P₁ = ").append(fmt(Pratio)).append("\n");
        sb.append("  ρ₂/ρ₁ = ").append(fmt(rhoRatio)).append("\n\n");
        sb.append("Note: expansion fans are isentropic — stagnation pressure is fully preserved (unlike shocks).");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Rocket Propulsion
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_rocket_propulsion",
          description = "Rocket propulsion calculations. "
                  + "Formulas: "
                  + "'tsiolkovsky' — ΔV from mass ratio (Isp, m₀, mf); "
                  + "'tsiolkovsky_mass' — propellant required for ΔV (Isp, ΔV, m₀); "
                  + "'thrust' — total thrust with pressure term (ṁ, Ve, Ae, Pe, Pa); "
                  + "'isp' — specific impulse from thrust and mass flow (F, ṁ); "
                  + "'exit_velocity' — isentropic nozzle exit velocity (γ, R_gas, Tc, Pc, Pe); "
                  + "'mass_flow' — propellant mass flow from thrust and Isp (F, Isp); "
                  + "'expansion_ratio' — nozzle area ratio Ae/At from exit Mach (Me, [γ]).")
    public String rocketPropulsion(
            @ToolParam(description = "Formula name (see description)") String formula,
            @ToolParam(description = "Comma-separated numeric values for the chosen formula") String values) {

        try {
            double[] v = parseValues(values);
            return switch (formula.strip().toLowerCase()) {

                case "tsiolkovsky" -> {
                    checkMin(v, 3, "Isp(s), m0(kg), mf(kg)");
                    double Isp = v[0], m0 = v[1], mf = v[2];
                    if (mf >= m0) yield "Error: mf must be < m0 (fuel is consumed).";
                    double Ve   = Isp * G0;
                    double dV   = Ve * Math.log(m0 / mf);
                    double mProp = m0 - mf;
                    yield "Tsiolkovsky Rocket Equation\n───────────────────────────\n"
                            + "Formula: ΔV = Isp·g₀·ln(m₀/mf)\n\n"
                            + "  Isp  = " + fmt(Isp) + " s\n"
                            + "  m₀   = " + fmt(m0) + " kg\n"
                            + "  mf   = " + fmt(mf) + " kg\n"
                            + "  Ve   = Isp·g₀    = " + fmt(Ve) + " m/s\n"
                            + "  m₀/mf            = " + fmt(m0 / mf) + "  (mass ratio)\n"
                            + "  Fuel fraction     = " + fmt((mProp / m0) * 100) + "%\n\n"
                            + "RESULT:\n"
                            + "  ΔV = " + fmt(dV) + " m/s  =  " + fmt(dV / 1000) + " km/s\n\n"
                            + "Reference ΔV: LEO ≈ 9.5 km/s, GTO ≈ 12 km/s, TLI ≈ 13.5 km/s";
                }

                case "tsiolkovsky_mass" -> {
                    checkMin(v, 3, "Isp(s), deltaV(m/s), m0(kg)");
                    double Isp = v[0], dV = v[1], m0 = v[2];
                    double Ve   = Isp * G0;
                    double mf   = m0 / Math.exp(dV / Ve);
                    double mProp = m0 - mf;
                    yield "Tsiolkovsky — Propellant Required\n──────────────────────────────────\n"
                            + "Formula: mf = m₀·exp(−ΔV/(Isp·g₀))\n\n"
                            + "  Isp = " + fmt(Isp) + " s\n"
                            + "  ΔV  = " + fmt(dV) + " m/s  (" + fmt(dV / 1000) + " km/s)\n"
                            + "  m₀  = " + fmt(m0) + " kg\n\n"
                            + "RESULT:\n"
                            + "  mf           = " + fmt(mf) + " kg  (final / dry mass)\n"
                            + "  m_propellant = " + fmt(mProp) + " kg\n"
                            + "  Fuel fraction = " + fmt((mProp / m0) * 100) + "%\n"
                            + "  Mass ratio m₀/mf = " + fmt(m0 / mf);
                }

                case "thrust" -> {
                    checkMin(v, 5, "mass_flow(kg/s), Ve(m/s), Ae(m²), Pe(Pa), Pa_ambient(Pa)");
                    double mdot = v[0], Ve = v[1], Ae = v[2], Pe = v[3], Pa = v[4];
                    double Fmom  = mdot * Ve;
                    double Fpress = (Pe - Pa) * Ae;
                    double Ftotal = Fmom + Fpress;
                    double Isp   = Ftotal / (mdot * G0);
                    yield "Rocket Thrust\n─────────────\n"
                            + "Formula: F = ṁ·Ve + (Pe−Pa)·Ae\n\n"
                            + "  ṁ  = " + fmt(mdot) + " kg/s\n"
                            + "  Ve = " + fmt(Ve) + " m/s\n"
                            + "  Ae = " + fmt(Ae) + " m²\n"
                            + "  Pe = " + fmt(Pe) + " Pa  (nozzle exit)\n"
                            + "  Pa = " + fmt(Pa) + " Pa  (ambient)\n\n"
                            + "RESULT:\n"
                            + "  Momentum thrust  ṁ·Ve        = " + fmt(Fmom) + " N\n"
                            + "  Pressure thrust  (Pe−Pa)·Ae  = " + fmt(Fpress) + " N\n"
                            + "  Total thrust  F              = " + fmt(Ftotal) + " N  (" + fmt(Ftotal / 1000) + " kN)\n"
                            + "  Isp = F/(ṁ·g₀)             = " + fmt(Isp) + " s";
                }

                case "isp" -> {
                    checkMin(v, 2, "thrust(N), mass_flow(kg/s)");
                    double Isp = v[0] / (v[1] * G0);
                    double Ve  = Isp * G0;
                    yield "Specific Impulse\n────────────────\n"
                            + "Formula: Isp = F/(ṁ·g₀)\n\n"
                            + "  F = " + fmt(v[0]) + " N,  ṁ = " + fmt(v[1]) + " kg/s\n\n"
                            + "RESULT:\n"
                            + "  Isp = " + fmt(Isp) + " s\n"
                            + "  Ve  = Isp·g₀ = " + fmt(Ve) + " m/s\n\n"
                            + "Reference: solid ≈ 250–300 s, RP-1/LOX ≈ 310–360 s, H₂/LOX ≈ 450 s, ion ≈ 1000–10000 s";
                }

                case "exit_velocity" -> {
                    checkMin(v, 5, "gamma, R_gas(J/kg·K), T_chamber(K), P_chamber(Pa), P_exit(Pa)");
                    double gam = v[0], R = v[1], Tc = v[2], Pc = v[3], Pe = v[4];
                    if (Pe >= Pc) yield "Error: exit pressure Pe must be < chamber pressure Pc.";
                    double Ve  = Math.sqrt(2 * gam / (gam - 1) * R * Tc * (1 - Math.pow(Pe / Pc, (gam - 1) / gam)));
                    double Me  = Math.sqrt((Math.pow(Pc / Pe, (gam - 1) / gam) - 1) * 2 / (gam - 1));
                    double Te  = Tc / (1 + (gam - 1) / 2 * Me * Me);
                    double Isp = Ve / G0;
                    yield "Nozzle Exit Velocity (Isentropic)\n──────────────────────────────────\n"
                            + "Formula: Ve = √(2γ/(γ-1)·R·Tc·(1−(Pe/Pc)^((γ-1)/γ)))\n\n"
                            + "  γ  = " + fmt(gam) + ",  R  = " + fmt(R) + " J/(kg·K)\n"
                            + "  Tc = " + fmt(Tc) + " K,  Pc = " + fmt(Pc) + " Pa,  Pe = " + fmt(Pe) + " Pa\n\n"
                            + "RESULT:\n"
                            + "  Ve  = " + fmt(Ve) + " m/s\n"
                            + "  Me  = " + fmt(Me) + "  (exit Mach)\n"
                            + "  Te  = " + fmt(Te) + " K  (exit temperature)\n"
                            + "  Isp ≈ Ve/g₀ = " + fmt(Isp) + " s  (pressure-matched)";
                }

                case "mass_flow" -> {
                    checkMin(v, 2, "thrust(N), Isp(s)");
                    double mdot = v[0] / (v[1] * G0);
                    yield "Propellant Mass Flow\n────────────────────\n"
                            + "Formula: ṁ = F/(Isp·g₀)\n\n"
                            + "  F = " + fmt(v[0]) + " N,  Isp = " + fmt(v[1]) + " s\n\n"
                            + "  ṁ = " + fmt(mdot) + " kg/s";
                }

                case "expansion_ratio" -> {
                    checkMin(v, 1, "exit_Mach_Me, [gamma=1.4]");
                    double Me  = v[0];
                    double gam = v.length > 1 ? v[1] : GAMMA_AIR;
                    if (Me < 1) yield "Error: exit Mach must be ≥ 1 for a supersonic nozzle.";
                    double base  = (2.0 / (gam + 1)) * (1 + (gam - 1) / 2 * Me * Me);
                    double exp   = (gam + 1) / (2 * (gam - 1));
                    double AeAt  = (1.0 / Me) * Math.pow(base, exp);
                    yield "Nozzle Expansion Ratio\n──────────────────────\n"
                            + "Formula: Ae/At = (1/Me)·((2/(γ+1))·(1+(γ-1)/2·Me²))^((γ+1)/(2(γ-1)))\n\n"
                            + "  Me = " + fmt(Me) + ",  γ = " + fmt(gam) + "\n\n"
                            + "  Ae/At = " + fmt(AeAt) + "\n"
                            + "  (At = throat area at M=1)";
                }

                default -> "Unknown formula. Use: tsiolkovsky, tsiolkovsky_mass, thrust, isp, exit_velocity, mass_flow, expansion_ratio";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Flight Performance
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_flight_performance",
          description = "Aircraft flight performance calculations. "
                  + "Formulas: "
                  + "'breguet_range' — jet aircraft range (V, Isp, L/D, m₀, mf); "
                  + "'breguet_endurance' — loiter time (Isp, L/D, m₀, mf); "
                  + "'stall_speed' — minimum flight speed (W, ρ, S, CL_max); "
                  + "'rate_of_climb' — vertical climb rate (T, D, V, W); "
                  + "'turn_bank' — sustained level turn from bank angle (V, φ°); "
                  + "'turn_load' — sustained level turn from load factor (V, n).")
    public String flightPerformance(
            @ToolParam(description = "Formula name (see description)") String formula,
            @ToolParam(description = "Comma-separated numeric values for the chosen formula") String values) {

        try {
            double[] v = parseValues(values);
            return switch (formula.strip().toLowerCase()) {

                case "breguet_range" -> {
                    checkMin(v, 5, "velocity(m/s), Isp(s), L/D_ratio, m_initial(kg), m_final(kg)");
                    double V = v[0], Isp = v[1], LD = v[2], m0 = v[3], mf = v[4];
                    if (mf >= m0) yield "Error: m_final must be < m_initial.";
                    double R = V * Isp * LD * Math.log(m0 / mf);
                    yield "Breguet Range (Jet)\n───────────────────\n"
                            + "Formula: R = V · Isp · (L/D) · ln(m₀/mf)\n\n"
                            + "  V   = " + fmt(V) + " m/s  (" + fmt(V * 3.6) + " km/h)\n"
                            + "  Isp = " + fmt(Isp) + " s\n"
                            + "  L/D = " + fmt(LD) + "\n"
                            + "  m₀  = " + fmt(m0) + " kg  (MTOW / initial)\n"
                            + "  mf  = " + fmt(mf) + " kg  (OEW / final)\n"
                            + "  Fuel fraction = " + fmt((m0 - mf) / m0 * 100) + "%\n\n"
                            + "RESULT:\n"
                            + "  R = " + fmt(R / 1000) + " km  =  " + fmt(R / 1852) + " NM";
                }

                case "breguet_endurance" -> {
                    checkMin(v, 4, "Isp(s), L/D_ratio, m_initial(kg), m_final(kg)");
                    double Isp = v[0], LD = v[1], m0 = v[2], mf = v[3];
                    if (mf >= m0) yield "Error: m_final must be < m_initial.";
                    double E = Isp * LD * Math.log(m0 / mf);
                    yield "Breguet Endurance (Loiter)\n──────────────────────────\n"
                            + "Formula: E = Isp · (L/D) · ln(m₀/mf)\n\n"
                            + "  Isp = " + fmt(Isp) + " s,  L/D = " + fmt(LD) + "\n"
                            + "  m₀  = " + fmt(m0) + " kg,  mf = " + fmt(mf) + " kg\n\n"
                            + "RESULT:\n"
                            + "  E = " + fmt(E) + " s  =  " + fmt(E / 3600) + " hours";
                }

                case "stall_speed" -> {
                    checkMin(v, 4, "weight(N), density(kg/m³), wing_area(m²), CL_max");
                    double W = v[0], rho = v[1], S = v[2], CLmax = v[3];
                    double Vs = Math.sqrt(2 * W / (rho * S * CLmax));
                    yield "Stall Speed\n───────────\n"
                            + "Formula: Vs = √(2W / (ρ·S·CL_max))\n\n"
                            + "  W      = " + fmt(W) + " N  (" + fmt(W / G0) + " kg)\n"
                            + "  ρ      = " + fmt(rho) + " kg/m³\n"
                            + "  S      = " + fmt(S) + " m²\n"
                            + "  CL_max = " + fmt(CLmax) + "\n\n"
                            + "RESULT:\n"
                            + "  Vs = " + fmt(Vs) + " m/s  =  " + fmt(Vs * 3.6) + " km/h  =  " + fmt(Vs * 1.944) + " knots";
                }

                case "rate_of_climb" -> {
                    checkMin(v, 4, "thrust(N), drag(N), velocity(m/s), weight(N)");
                    double T = v[0], D = v[1], V = v[2], W = v[3];
                    double excessThrust = T - D;
                    double ROC = excessThrust * V / W;
                    double climbAngle = Math.toDegrees(Math.asin(Math.min(1.0, Math.max(-1.0, excessThrust / W))));
                    yield "Rate of Climb\n─────────────\n"
                            + "Formula: ROC = (T−D)·V/W\n\n"
                            + "  T = " + fmt(T) + " N,  D = " + fmt(D) + " N\n"
                            + "  V = " + fmt(V) + " m/s,  W = " + fmt(W) + " N\n"
                            + "  Excess thrust = T−D = " + fmt(excessThrust) + " N\n"
                            + "  Excess power  = " + fmt(excessThrust * V / 1000) + " kW\n\n"
                            + "RESULT:\n"
                            + "  ROC          = " + fmt(ROC) + " m/s  =  " + fmt(ROC * 196.85) + " ft/min\n"
                            + "  Climb angle γ = arcsin((T−D)/W) = " + fmt(climbAngle) + "°\n"
                            + (ROC < 0 ? "  (Negative ROC → descending)" : "");
                }

                case "turn_bank" -> {
                    checkMin(v, 2, "velocity(m/s), bank_angle(degrees)");
                    double V = v[0], phi = v[1];
                    if (phi < 0 || phi >= 90) yield "Error: bank angle must be in [0°, 90°).";
                    double n     = 1.0 / Math.cos(Math.toRadians(phi));
                    double omega = G0 * Math.sqrt(n * n - 1) / V;
                    double r     = V * V / (G0 * Math.sqrt(n * n - 1));
                    yield turnResult(V, phi, n, omega, r);
                }

                case "turn_load" -> {
                    checkMin(v, 2, "velocity(m/s), load_factor_n");
                    double V = v[0], n = v[1];
                    if (n < 1) yield "Error: load factor n must be ≥ 1 for a level turn.";
                    double phi   = Math.toDegrees(Math.acos(1.0 / n));
                    double omega = G0 * Math.sqrt(n * n - 1) / V;
                    double r     = V * V / (G0 * Math.sqrt(n * n - 1));
                    yield turnResult(V, phi, n, omega, r);
                }

                default -> "Unknown formula. Use: breguet_range, breguet_endurance, stall_speed, rate_of_climb, turn_bank, turn_load";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Aerodynamic Heating
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_heating",
          description = "Aerodynamic / re-entry heating analysis. "
                  + "Computes stagnation temperature T₀, recovery (adiabatic wall) temperature Tr, "
                  + "stagnation-point convective heat flux (Chapman/DKR formula), and "
                  + "radiation equilibrium wall temperature. "
                  + "Use for: TPS sizing, re-entry vehicle thermal analysis, hypersonic skin temperature estimation. "
                  + "Chapman formula accuracy ±30%; use CFD for design-level precision.")
    public String aerodynamicHeating(
            @ToolParam(description = "Flight Mach number M∞") Double mach,
            @ToolParam(description = "Flight velocity V∞ in m/s") Double velocity,
            @ToolParam(description = "Freestream static temperature T∞ in K") Double staticTemp,
            @ToolParam(description = "Freestream density ρ∞ in kg/m³") Double density,
            @ToolParam(description = "Nose radius rN in m (required for stagnation heat flux; omit to skip)", required = false) Double noseRadius,
            @ToolParam(description = "Wall temperature Tw in K (default 300 K — cold structure)", required = false) Double wallTemp,
            @ToolParam(description = "Surface emissivity ε, 0–1 (default 0.85, TPS-like). "
                    + "Used to compute radiation equilibrium temperature.", required = false) Double emissivity,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (mach == null || velocity == null || staticTemp == null || density == null)
            return "Error: mach, velocity, staticTemp, and density are all required.";
        if (mach < 0) return "Error: Mach must be non-negative.";

        double g   = (gamma != null)     ? gamma     : GAMMA_AIR;
        double Tw  = (wallTemp != null)  ? wallTemp  : 300.0;
        double eps = (emissivity != null)? emissivity: 0.85;

        double rLam  = Math.sqrt(PR_AIR);           // recovery factor, laminar  ≈ 0.849
        double rTurb = Math.pow(PR_AIR, 1.0 / 3.0); // recovery factor, turbulent ≈ 0.896

        double T0      = staticTemp * (1 + (g - 1) / 2.0 * mach * mach);
        double Tr_lam  = staticTemp * (1 + rLam  * (g - 1) / 2.0 * mach * mach);
        double Tr_turb = staticTemp * (1 + rTurb * (g - 1) / 2.0 * mach * mach);

        StringBuilder sb = new StringBuilder();
        sb.append("Aerodynamic Heating Analysis\n────────────────────────────\n\n");
        sb.append("FREESTREAM:\n");
        sb.append("  M∞  = ").append(fmt(mach)).append("\n");
        sb.append("  V∞  = ").append(fmt(velocity)).append(" m/s\n");
        sb.append("  T∞  = ").append(fmt(staticTemp)).append(" K\n");
        sb.append("  ρ∞  = ").append(fmt(density)).append(" kg/m³\n\n");
        sb.append("TEMPERATURES:\n");
        sb.append("  T₀        = T∞·(1+(γ-1)/2·M²) = ").append(fmt(T0)).append(" K  (stagnation)\n");
        sb.append("  Tr (lam)  = ").append(fmt(Tr_lam)).append(" K  (adiabatic wall, r = Pr^0.5 = ").append(fmt(rLam)).append(")\n");
        sb.append("  Tr (turb) = ").append(fmt(Tr_turb)).append(" K  (adiabatic wall, r = Pr^1/3 = ").append(fmt(rTurb)).append(")\n");
        sb.append("  Tw        = ").append(fmt(Tw)).append(" K  (assumed wall temperature)\n\n");

        if (noseRadius != null) {
            if (noseRadius <= 0) {
                sb.append("Error: noseRadius must be positive.\n");
            } else {
                double hFactor = 1.0 - Tw / T0;
                if (hFactor < 0) hFactor = 0;
                double qStag  = 1.83e-4 * Math.sqrt(density / noseRadius) * Math.pow(velocity, 3) * hFactor;
                double TwRad  = Math.pow(qStag / (eps * SIGMA), 0.25);

                sb.append("STAGNATION-POINT HEAT FLUX (Chapman formula):\n");
                sb.append("  rN  = ").append(fmt(noseRadius * 1000)).append(" mm\n");
                sb.append("  (1 − Tw/T₀) = ").append(fmt(hFactor)).append("  (enthalpy driving factor)\n");
                sb.append("  q_s = 1.83×10⁻⁴·√(ρ∞/rN)·V∞³·(1−Tw/T₀)\n");
                sb.append("  q_s = ").append(sci(qStag)).append(" W/m²");
                if (qStag >= 1e6) sb.append("  =  ").append(fmt(qStag / 1e6)).append(" MW/m²");
                sb.append("\n\n");
                sb.append("RADIATION EQUILIBRIUM:  ε·σ·Tw_rad⁴ = q_s\n");
                sb.append("  ε      = ").append(fmt(eps)).append("\n");
                sb.append("  Tw_rad = (q_s/(ε·σ))^0.25 = ").append(fmt(TwRad)).append(" K  (").append(fmt(TwRad - 273.15)).append(" °C)\n");
            }
        }

        sb.append("\nNote: Chapman formula is empirical (±30%). Fay-Riddell or CFD required for design precision.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Turbomachinery
    // ─────────────────────────────────────────────────────────────────────────

    @Tool(name = "aero_turbomachinery",
          description = "Axial compressor and turbine stage analysis. "
                  + "Formulas: "
                  + "'compressor' — isentropic stage (T_in, P_in, PR, η_c, [ṁ]); "
                  + "'turbine' — isentropic stage (T_in, P_in, PR, η_t, [ṁ]); "
                  + "'euler' — velocity triangle work (U, Cw₁, Cw₂, Ca). "
                  + "For compressor: PR = P_out/P_in > 1. For turbine: PR = P_in/P_out > 1. "
                  + "Uses γ = 1.4, cp = 1004.5 J/(kg·K) for air.")
    public String turbomachinery(
            @ToolParam(description = "Formula: 'compressor', 'turbine', or 'euler'") String formula,
            @ToolParam(description = "Comma-separated values:\n"
                    + "  compressor: T01(K), P01(Pa), PR(>1), eta_c(0–1), [mass_flow(kg/s)]\n"
                    + "  turbine   : T03(K), P03(Pa), PR(>1), eta_t(0–1), [mass_flow(kg/s)]\n"
                    + "  euler     : blade_speed_U(m/s), whirl_in_Cw1(m/s), whirl_out_Cw2(m/s), axial_Ca(m/s)") String values) {

        try {
            double[] v = parseValues(values);
            double g = GAMMA_AIR;

            return switch (formula.strip().toLowerCase()) {

                case "compressor" -> {
                    checkMin(v, 4, "T01(K), P01(Pa), PR, eta_c [, mass_flow(kg/s)]");
                    double T01 = v[0], P01 = v[1], PR = v[2], eta = v[3];
                    if (PR <= 1) yield "Error: compressor pressure ratio PR must be > 1.";
                    if (eta <= 0 || eta > 1) yield "Error: isentropic efficiency must be in (0, 1].";
                    double T02s = T01 * Math.pow(PR, (g - 1) / g);
                    double T02  = T01 + (T02s - T01) / eta;
                    double P02  = PR * P01;
                    double work = CP_AIR * (T02 - T01);
                    String powerLine = v.length > 4
                            ? "  Power = ṁ·W = " + fmt(v[4] * work / 1e6) + " MW  (ṁ = " + fmt(v[4]) + " kg/s)\n"
                            : "";
                    yield "Compressor Stage (Isentropic)\n──────────────────────────────\n"
                            + "GIVEN:\n"
                            + "  T₀₁ = " + fmt(T01) + " K\n"
                            + "  P₀₁ = " + fmt(P01) + " Pa\n"
                            + "  PR  = P₀₂/P₀₁ = " + fmt(PR) + "\n"
                            + "  η_c = " + fmt(eta * 100) + "%\n\n"
                            + "RESULT:\n"
                            + "  T₀₂s = T₀₁·PR^((γ-1)/γ)     = " + fmt(T02s) + " K  (ideal)\n"
                            + "  T₀₂  = T₀₁ + (T₀₂s-T₀₁)/η_c = " + fmt(T02) + " K  (actual)\n"
                            + "  P₀₂  = PR·P₀₁ = " + fmt(P02) + " Pa\n"
                            + "  ΔT₀  = " + fmt(T02 - T01) + " K\n"
                            + "  W    = cp·ΔT₀ = " + fmt(work / 1000) + " kJ/kg\n"
                            + powerLine;
                }

                case "turbine" -> {
                    checkMin(v, 4, "T03(K), P03(Pa), PR(P03/P04), eta_t [, mass_flow(kg/s)]");
                    double T03 = v[0], P03 = v[1], PR = v[2], eta = v[3];
                    if (PR <= 1) yield "Error: turbine expansion ratio PR = P_in/P_out must be > 1.";
                    if (eta <= 0 || eta > 1) yield "Error: isentropic efficiency must be in (0, 1].";
                    double T04s = T03 / Math.pow(PR, (g - 1) / g);
                    double T04  = T03 - eta * (T03 - T04s);
                    double P04  = P03 / PR;
                    double work = CP_AIR * (T03 - T04);
                    String powerLine = v.length > 4
                            ? "  Power = ṁ·W = " + fmt(v[4] * work / 1e6) + " MW  (ṁ = " + fmt(v[4]) + " kg/s)\n"
                            : "";
                    yield "Turbine Stage (Isentropic)\n──────────────────────────\n"
                            + "GIVEN:\n"
                            + "  T₀₃ = " + fmt(T03) + " K\n"
                            + "  P₀₃ = " + fmt(P03) + " Pa\n"
                            + "  PR  = P₀₃/P₀₄ = " + fmt(PR) + "\n"
                            + "  η_t = " + fmt(eta * 100) + "%\n\n"
                            + "RESULT:\n"
                            + "  T₀₄s = T₀₃/PR^((γ-1)/γ)      = " + fmt(T04s) + " K  (ideal)\n"
                            + "  T₀₄  = T₀₃ − η·(T₀₃−T₀₄s) = " + fmt(T04) + " K  (actual)\n"
                            + "  P₀₄  = P₀₃/PR = " + fmt(P04) + " Pa\n"
                            + "  ΔT₀  = " + fmt(T03 - T04) + " K  (temperature drop)\n"
                            + "  W    = cp·ΔT₀ = " + fmt(work / 1000) + " kJ/kg\n"
                            + powerLine;
                }

                case "euler" -> {
                    checkMin(v, 4, "U(m/s), Cw1(m/s), Cw2(m/s), Ca(m/s)");
                    double U   = v[0], Cw1 = v[1], Cw2 = v[2], Ca = v[3];
                    if (U <= 0) yield "Error: blade speed U must be positive.";
                    if (Ca <= 0) yield "Error: axial velocity Ca must be positive.";
                    double W   = U * (Cw2 - Cw1);
                    double psi = (Cw2 - Cw1) / U;
                    double phi = Ca / U;
                    double a1  = Math.toDegrees(Math.atan2(Cw1, Ca));
                    double a2  = Math.toDegrees(Math.atan2(Cw2, Ca));
                    double b1  = Math.toDegrees(Math.atan2(U - Cw1, Ca));
                    double b2  = Math.toDegrees(Math.atan2(U - Cw2, Ca));
                    String mode = W >= 0 ? "compressor (work input +)" : "turbine (work output −)";
                    yield "Euler Turbomachinery Equation  [" + mode + "]\n"
                            + "─────────────────────────────────────────────\n"
                            + "Formula: W = U·(Cw₂ − Cw₁)\n\n"
                            + "  U   = " + fmt(U) + " m/s  (blade speed)\n"
                            + "  Cw₁ = " + fmt(Cw1) + " m/s  (inlet whirl)\n"
                            + "  Cw₂ = " + fmt(Cw2) + " m/s  (outlet whirl)\n"
                            + "  Ca  = " + fmt(Ca) + " m/s  (axial velocity)\n\n"
                            + "RESULT:\n"
                            + "  W   = " + fmt(W / 1000) + " kJ/kg\n"
                            + "  ψ   = ΔCw/U = " + fmt(psi) + "  (stage loading)\n"
                            + "  φ   = Ca/U  = " + fmt(phi) + "  (flow coefficient)\n\n"
                            + "VELOCITY TRIANGLE ANGLES (from axial direction):\n"
                            + "  α₁ = " + fmt(a1) + "°  (absolute inlet)\n"
                            + "  α₂ = " + fmt(a2) + "°  (absolute outlet)\n"
                            + "  β₁ = " + fmt(b1) + "°  (relative inlet)\n"
                            + "  β₂ = " + fmt(b2) + "°  (relative outlet)\n";
                }

                default -> "Unknown formula. Use: compressor, turbine, euler";
            };
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Numerical helpers — oblique shock
    // ─────────────────────────────────────────────────────────────────────────

    private double thetaFromBetaRad(double M1, double betaRad, double gamma) {
        double sinB = Math.sin(betaRad);
        double cosB = Math.cos(betaRad);
        double numer = M1 * M1 * sinB * sinB - 1;
        double denom = M1 * M1 * (gamma + Math.cos(2 * betaRad)) + 2;
        if (denom == 0 || numer <= 0) return 0;
        return Math.atan(2 * (cosB / sinB) * (numer / denom));
    }

    private double findBetaAtMaxTheta(double M1, double gamma) {
        double mu = Math.asin(1.0 / M1);
        double betaBest = mu, thetaMax = 0;
        int steps = 5000;
        for (int i = 1; i <= steps; i++) {
            double beta  = mu + (Math.PI / 2 - mu) * i / steps;
            double theta = thetaFromBetaRad(M1, beta, gamma);
            if (theta > thetaMax) { thetaMax = theta; betaBest = beta; }
        }
        return betaBest;
    }

    private double findWeakBeta(double M1, double thetaRad, double gamma) {
        double mu      = Math.asin(1.0 / M1);
        double betaMax = findBetaAtMaxTheta(M1, gamma);
        double lo = mu, hi = betaMax;
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2;
            if (thetaFromBetaRad(M1, mid, gamma) < thetaRad) lo = mid;
            else hi = mid;
        }
        return (lo + hi) / 2;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Numerical helpers — Prandtl-Meyer
    // ─────────────────────────────────────────────────────────────────────────

    double pmFunction(double M, double gamma) {
        if (M < 1.0) return 0;
        double g1 = gamma - 1, g2 = gamma + 1;
        return Math.sqrt(g2 / g1) * Math.atan(Math.sqrt(g1 / g2 * (M * M - 1)))
               - Math.atan(Math.sqrt(M * M - 1));
    }

    double mFromPM(double nu, double gamma) {
        double lo = 1.0, hi = 200.0;
        for (int i = 0; i < 100; i++) {
            double mid = (lo + hi) / 2;
            if (pmFunction(mid, gamma) < nu) lo = mid;
            else hi = mid;
        }
        return (lo + hi) / 2;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String turnResult(double V, double phi, double n, double omega, double r) {
        return "Level Turn Performance\n───────────────────────\n"
                + "Formulas: n = 1/cos(φ),  ω = g·√(n²−1)/V,  r = V²/(g·√(n²−1))\n\n"
                + "  V = " + fmt(V) + " m/s  (" + fmt(V * 3.6) + " km/h)\n"
                + "  Bank angle φ  = " + fmt(phi) + "°\n"
                + "  Load factor n = " + fmt(n) + " g\n\n"
                + "RESULT:\n"
                + "  Turn rate ω   = " + fmt(Math.toDegrees(omega)) + " °/s\n"
                + "  Turn radius r = " + fmt(r) + " m  (" + fmt(r / 1000) + " km)";
    }

    private double[] parseValues(String values) {
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Double.parseDouble(parts[i].strip());
        return result;
    }

    private void checkMin(double[] v, int min, String names) {
        if (v.length < min)
            throw new IllegalArgumentException("Need at least " + min + " values: " + names);
    }

    private String fmt(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (v == Math.floor(v) && Math.abs(v) < 1e12) return String.valueOf((long) v);
        return BigDecimal.valueOf(v).round(MC).stripTrailingZeros().toPlainString();
    }

    private String sci(double v) {
        return String.format("%.4e", v);
    }
}
