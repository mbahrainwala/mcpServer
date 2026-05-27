package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MCP tool for computational fluid dynamics calculations relevant to aerospace engineering.
 * Covers incompressible/compressible flow, aerodynamics, shock waves, pipe flow, and more.
 */
@Service
public class FluidDynamicsTool {

    private static final MathContext MC = new MathContext(10);

    // Standard air properties at sea level (ISA)
    private static final double RHO_AIR_SL  = 1.225;       // density at sea level (kg/m³)
    private static final double P_SL         = 101325.0;    // pressure at sea level (Pa)
    private static final double T_SL         = 288.15;      // temperature at sea level (K)
    private static final double MU_AIR_SL    = 1.789e-5;    // dynamic viscosity at sea level (Pa·s)
    private static final double GAMMA_AIR    = 1.4;         // ratio of specific heats for air
    private static final double R_AIR        = 287.058;     // specific gas constant for air (J/kg·K)
    private static final double A_SL         = 340.294;     // speed of sound at sea level (m/s)

    @Tool(name = "fluid_bernoulli",
          description = "Bernoulli's equation for steady, incompressible, inviscid flow. "
                  + "Given any two states (pressure, velocity, height) on a streamline, "
                  + "solves for the unknown at state 2. Also computes dynamic pressure and stagnation pressure. "
                  + "Formula: P + ½ρv² + ρgh = const. "
                  + "Use for: airspeed from pitot-static probes, venturi meters, nozzle flow.")
    public String bernoulli(
            @ToolParam(description = "Fluid density in kg/m³ (e.g. 1.225 for air at sea level, 1000 for water)") Double density,
            @ToolParam(description = "Static pressure at point 1 in Pa") Double p1,
            @ToolParam(description = "Flow velocity at point 1 in m/s") Double v1,
            @ToolParam(description = "Elevation at point 1 in m (use 0 if horizontal flow)") Double h1,
            @ToolParam(description = "Static pressure at point 2 in Pa (null to solve for it)", required = false) Double p2,
            @ToolParam(description = "Flow velocity at point 2 in m/s (null to solve for it)", required = false) Double v2,
            @ToolParam(description = "Elevation at point 2 in m (use 0 if horizontal flow)", required = false) Double h2) {

        if (density == null || p1 == null || v1 == null || h1 == null) {
            return "Error: density, p1, v1, h1 are required. Provide one of p2, v2, h2 as null to solve for it.";
        }

        double g = 9.80665;
        double h2val = (h2 != null) ? h2 : h1;
        double bernoulliConst = p1 + 0.5 * density * v1 * v1 + density * g * h1;
        double dynP1 = 0.5 * density * v1 * v1;

        StringBuilder sb = new StringBuilder();
        sb.append("Bernoulli's Equation (Incompressible)\n");
        sb.append("─────────────────────────────────────\n\n");
        sb.append("Formula: P + ½ρv² + ρgh = const\n\n");
        sb.append("STATE 1:\n");
        sb.append("  P₁ = ").append(fmt(p1)).append(" Pa\n");
        sb.append("  v₁ = ").append(fmt(v1)).append(" m/s\n");
        sb.append("  h₁ = ").append(fmt(h1)).append(" m\n");
        sb.append("  q₁ = ½ρv₁² = ").append(fmt(dynP1)).append(" Pa  (dynamic pressure)\n");
        sb.append("  P₀₁ = P₁ + q₁ = ").append(fmt(p1 + dynP1)).append(" Pa  (stagnation pressure)\n");
        sb.append("  Bernoulli const = ").append(fmt(bernoulliConst)).append(" Pa\n\n");

        sb.append("STATE 2 (h₂ = ").append(fmt(h2val)).append(" m):\n");

        if (p2 == null && v2 != null) {
            double p2solved = bernoulliConst - 0.5 * density * v2 * v2 - density * g * h2val;
            double dynP2 = 0.5 * density * v2 * v2;
            sb.append("  v₂ = ").append(fmt(v2)).append(" m/s  (given)\n");
            sb.append("  P₂ = ").append(fmt(p2solved)).append(" Pa  (solved)\n");
            sb.append("  q₂ = ½ρv₂² = ").append(fmt(dynP2)).append(" Pa\n");
            sb.append("  ΔP = P₁ - P₂ = ").append(fmt(p1 - p2solved)).append(" Pa\n");
        } else if (v2 == null && p2 != null) {
            double v2sq = 2.0 * (bernoulliConst - p2 - density * g * h2val) / density;
            if (v2sq < 0) return "Error: Given P₂ is too high — v₂² < 0 (energy conservation violated).";
            double v2solved = Math.sqrt(v2sq);
            double dynP2 = 0.5 * density * v2solved * v2solved;
            sb.append("  P₂ = ").append(fmt(p2)).append(" Pa  (given)\n");
            sb.append("  v₂ = ").append(fmt(v2solved)).append(" m/s  (solved)\n");
            sb.append("  q₂ = ½ρv₂² = ").append(fmt(dynP2)).append(" Pa\n");
            sb.append("  ΔP = P₁ - P₂ = ").append(fmt(p1 - p2)).append(" Pa\n");
        } else if (p2 == null && v2 == null) {
            sb.append("  (provide either P₂ or v₂ to solve for the other)\n");
        } else {
            double lhs = p2 + 0.5 * density * v2 * v2 + density * g * h2val;
            sb.append("  P₂ = ").append(fmt(p2)).append(" Pa\n");
            sb.append("  v₂ = ").append(fmt(v2)).append(" m/s\n");
            sb.append("  Bernoulli check: ").append(fmt(lhs)).append(" Pa  (should equal ").append(fmt(bernoulliConst)).append(" Pa)\n");
            sb.append("  Error: ").append(fmt(Math.abs(lhs - bernoulliConst))).append(" Pa\n");
        }

        sb.append("\nρ = ").append(fmt(density)).append(" kg/m³,  g = ").append(fmt(g)).append(" m/s²");
        return sb.toString();
    }

    @Tool(name = "fluid_reynolds",
          description = "Calculate the Reynolds number and classify the flow regime. "
                  + "Re = ρvL/μ = vL/ν. "
                  + "Determines whether flow is laminar (Re < 2300), transitional (2300–4000), "
                  + "or turbulent (Re > 4000) for pipe flow. "
                  + "For flat plate / external flow, transition at Re ≈ 5×10⁵. "
                  + "Essential for boundary layer analysis and drag estimation.")
    public String reynolds(
            @ToolParam(description = "Flow velocity in m/s") Double velocity,
            @ToolParam(description = "Characteristic length in m (pipe diameter, chord length, plate length, etc.)") Double length,
            @ToolParam(description = "Fluid density in kg/m³ (null to use kinematic viscosity directly)", required = false) Double density,
            @ToolParam(description = "Dynamic viscosity in Pa·s (null to use kinematic viscosity directly)", required = false) Double dynamicViscosity,
            @ToolParam(description = "Kinematic viscosity in m²/s (ν = μ/ρ). Used if density/dynamicViscosity not provided. "
                    + "Defaults to 1.461×10⁻⁵ m²/s (air at sea level, 15°C).", required = false) Double kinematicViscosity) {

        if (velocity == null || length == null) return "Error: velocity and length are required.";

        double nu;
        if (density != null && dynamicViscosity != null) {
            nu = dynamicViscosity / density;
        } else if (kinematicViscosity != null) {
            nu = kinematicViscosity;
        } else {
            nu = MU_AIR_SL / RHO_AIR_SL; // default: air at sea level
        }

        double Re = velocity * length / nu;

        String pipeRegime, externalRegime;
        if (Re < 2300) pipeRegime = "LAMINAR (Re < 2300)";
        else if (Re < 4000) pipeRegime = "TRANSITIONAL (2300 < Re < 4000)";
        else pipeRegime = "TURBULENT (Re > 4000)";

        if (Re < 5e5) externalRegime = "LAMINAR boundary layer (Re < 5×10⁵)";
        else externalRegime = "TURBULENT boundary layer (Re > 5×10⁵)";

        // Laminar flat-plate boundary layer thickness at trailing edge (Blasius)
        double blasiusDelta = 5.0 * length / Math.sqrt(Re);
        // Skin friction coefficient (flat plate, laminar): Cf = 1.328 / sqrt(Re_L)
        double cfLam = 1.328 / Math.sqrt(Re);
        // Turbulent: Cf ≈ 0.074 / Re^0.2
        double cfTurb = 0.074 / Math.pow(Re, 0.2);

        StringBuilder sb = new StringBuilder();
        sb.append("Reynolds Number Analysis\n");
        sb.append("────────────────────────\n\n");
        sb.append("Formula: Re = vL/ν\n\n");
        sb.append("GIVEN:\n");
        sb.append("  v = ").append(fmt(velocity)).append(" m/s\n");
        sb.append("  L = ").append(fmt(length)).append(" m\n");
        sb.append("  ν = ").append(sci(nu)).append(" m²/s");
        if (density != null && dynamicViscosity != null) {
            sb.append("  (μ/ρ = ").append(sci(dynamicViscosity)).append("/").append(fmt(density)).append(")");
        }
        sb.append("\n\n");
        sb.append("RESULT:\n");
        sb.append("  Re = ").append(fmt(Re)).append("\n\n");
        sb.append("FLOW REGIME:\n");
        sb.append("  Pipe / internal flow : ").append(pipeRegime).append("\n");
        sb.append("  External / flat plate: ").append(externalRegime).append("\n\n");
        sb.append("BOUNDARY LAYER (flat plate, Blasius):\n");
        sb.append("  δ at x=L = 5L/√Re = ").append(fmt(blasiusDelta * 1000)).append(" mm\n");
        sb.append("  Cf (laminar)  = 1.328/√Re = ").append(sci(cfLam)).append("\n");
        sb.append("  Cf (turbulent) ≈ 0.074/Re^0.2 = ").append(sci(cfTurb)).append("\n");

        return sb.toString();
    }

    @Tool(name = "fluid_mach",
          description = "Calculate the Mach number and compressibility parameters for gas flow. "
                  + "M = v/a, where a = √(γRT) is the local speed of sound. "
                  + "Classifies flow as subsonic, transonic, supersonic, or hypersonic. "
                  + "Computes stagnation temperature and pressure using isentropic relations.")
    public String mach(
            @ToolParam(description = "Flow velocity in m/s") Double velocity,
            @ToolParam(description = "Static temperature in K (e.g. 288.15 for ISA sea level). "
                    + "If null, uses ISA sea-level temperature.", required = false) Double temperature,
            @ToolParam(description = "Static pressure in Pa (used for stagnation pressure). "
                    + "If null, uses ISA sea-level pressure.", required = false) Double pressure,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (velocity == null) return "Error: velocity is required.";

        double T = (temperature != null) ? temperature : T_SL;
        double P = (pressure != null) ? pressure : P_SL;
        double g = (gamma != null) ? gamma : GAMMA_AIR;

        double a = Math.sqrt(g * R_AIR * T);
        double M = velocity / a;

        String regime;
        if (M < 0.3) regime = "Incompressible (M < 0.3 — compressibility effects < 5%)";
        else if (M < 0.8) regime = "Subsonic (0.3 ≤ M < 0.8)";
        else if (M < 1.2) regime = "Transonic (0.8 ≤ M < 1.2)";
        else if (M < 5.0) regime = "Supersonic (1.2 ≤ M < 5.0)";
        else regime = "Hypersonic (M ≥ 5.0)";

        // Isentropic stagnation relations
        double isoFactor = 1.0 + (g - 1.0) / 2.0 * M * M;
        double T0 = T * isoFactor;                     // stagnation temperature
        double P0 = P * Math.pow(isoFactor, g / (g - 1.0)); // stagnation pressure
        double rho = P / (R_AIR * T);                  // static density
        double rho0 = rho * Math.pow(isoFactor, 1.0 / (g - 1.0)); // stagnation density
        double dynP = 0.5 * g * P * M * M;             // dynamic pressure q = ½γPM²

        // Critical (sonic) conditions
        double aStar = a * Math.sqrt(2.0 * isoFactor / (g + 1.0)); // a* at M=1 given same stagnation
        double TStar = T0 * 2.0 / (g + 1.0);
        double PStar = P0 * Math.pow(2.0 / (g + 1.0), g / (g - 1.0));

        StringBuilder sb = new StringBuilder();
        sb.append("Mach Number & Compressible Flow\n");
        sb.append("────────────────────────────────\n\n");
        sb.append("GIVEN:\n");
        sb.append("  v = ").append(fmt(velocity)).append(" m/s\n");
        sb.append("  T = ").append(fmt(T)).append(" K\n");
        sb.append("  P = ").append(fmt(P)).append(" Pa\n");
        sb.append("  γ = ").append(fmt(g)).append("\n\n");
        sb.append("RESULT:\n");
        sb.append("  a = √(γRT) = ").append(fmt(a)).append(" m/s  (speed of sound)\n");
        sb.append("  M = v/a    = ").append(fmt(M)).append("\n");
        sb.append("  Regime     : ").append(regime).append("\n\n");
        sb.append("ISENTROPIC STAGNATION (total) CONDITIONS:\n");
        sb.append("  T₀ = T(1 + (γ-1)/2·M²)         = ").append(fmt(T0)).append(" K\n");
        sb.append("  P₀ = P(1 + (γ-1)/2·M²)^(γ/γ-1) = ").append(fmt(P0)).append(" Pa\n");
        sb.append("  ρ₀ = P₀/(R·T₀)                  = ").append(fmt(rho0)).append(" kg/m³\n");
        sb.append("  q  = ½γPM²                       = ").append(fmt(dynP)).append(" Pa  (dynamic pressure)\n\n");
        sb.append("CRITICAL (SONIC) CONDITIONS (at M=1):\n");
        sb.append("  T* = ").append(fmt(TStar)).append(" K\n");
        sb.append("  P* = ").append(fmt(PStar)).append(" Pa\n");
        sb.append("  a* = ").append(fmt(aStar)).append(" m/s\n");

        return sb.toString();
    }

    @Tool(name = "fluid_isentropic",
          description = "Isentropic compressible flow relations for a nozzle or duct. "
                  + "Given Mach number and stagnation (total) conditions, computes static temperature, "
                  + "pressure, density, area ratio A/A*, and flow properties. "
                  + "Covers both subsonic and supersonic branches. "
                  + "Use for convergent-divergent nozzle design, inlet analysis, and de Laval nozzle calculations.")
    public String isentropic(
            @ToolParam(description = "Local Mach number M (any positive value)") Double M,
            @ToolParam(description = "Stagnation temperature T₀ in K") Double T0,
            @ToolParam(description = "Stagnation pressure P₀ in Pa") Double P0,
            @ToolParam(description = "Stagnation density ρ₀ in kg/m³ (optional; computed if P₀ and T₀ provided)", required = false) Double rho0,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (M == null || T0 == null || P0 == null) return "Error: M, T0, and P0 are required.";
        if (M < 0) return "Error: Mach number must be non-negative.";

        double g = (gamma != null) ? gamma : GAMMA_AIR;
        double rho0val = (rho0 != null) ? rho0 : P0 / (R_AIR * T0);

        double factor = 1.0 + (g - 1.0) / 2.0 * M * M;
        double T = T0 / factor;
        double P = P0 / Math.pow(factor, g / (g - 1.0));
        double rho = rho0val / Math.pow(factor, 1.0 / (g - 1.0));
        double a = Math.sqrt(g * R_AIR * T);
        double v = M * a;

        // Area ratio A/A* = (1/M) * [(2/(γ+1)) * (1 + (γ-1)/2 * M²)]^((γ+1)/(2(γ-1)))
        double aRatioBase = (2.0 / (g + 1.0)) * factor;
        double aRatioExp = (g + 1.0) / (2.0 * (g - 1.0));
        double aRatio = (M == 0) ? Double.POSITIVE_INFINITY : (1.0 / M) * Math.pow(aRatioBase, aRatioExp);

        // Mass flow parameter: ṁ√T₀/(A·P₀) = M·√(γ/R)·(1+(γ-1)/2·M²)^(-(γ+1)/(2(γ-1)))
        double mfp = M * Math.sqrt(g / R_AIR) * Math.pow(factor, -(g + 1.0) / (2.0 * (g - 1.0)));

        StringBuilder sb = new StringBuilder();
        sb.append("Isentropic Flow Relations\n");
        sb.append("─────────────────────────\n\n");
        sb.append("GIVEN:\n");
        sb.append("  M  = ").append(fmt(M)).append("\n");
        sb.append("  T₀ = ").append(fmt(T0)).append(" K\n");
        sb.append("  P₀ = ").append(fmt(P0)).append(" Pa\n");
        sb.append("  ρ₀ = ").append(fmt(rho0val)).append(" kg/m³\n");
        sb.append("  γ  = ").append(fmt(g)).append("\n\n");
        sb.append("STATIC CONDITIONS:\n");
        sb.append("  T   = T₀/(1+(γ-1)/2·M²)         = ").append(fmt(T)).append(" K\n");
        sb.append("  P   = P₀/(1+(γ-1)/2·M²)^(γ/γ-1) = ").append(fmt(P)).append(" Pa\n");
        sb.append("  ρ   = ρ₀/(1+(γ-1)/2·M²)^(1/γ-1) = ").append(fmt(rho)).append(" kg/m³\n");
        sb.append("  T/T₀ = ").append(fmt(T / T0)).append("\n");
        sb.append("  P/P₀ = ").append(fmt(P / P0)).append("\n");
        sb.append("  ρ/ρ₀ = ").append(fmt(rho / rho0val)).append("\n\n");
        sb.append("FLOW PROPERTIES:\n");
        sb.append("  a   = √(γRT) = ").append(fmt(a)).append(" m/s  (local speed of sound)\n");
        sb.append("  v   = M·a    = ").append(fmt(v)).append(" m/s  (flow velocity)\n\n");
        sb.append("AREA RATIO:\n");
        sb.append("  A/A* = ").append(fmt(aRatio)).append("\n");
        sb.append("  (A* is the throat area where M=1)\n\n");
        sb.append("MASS FLOW PARAMETER:\n");
        sb.append("  ṁ√T₀/(A·P₀) = ").append(sci(mfp)).append(" kg/(s·m²·Pa·K^0.5·√(J/kg·K))");

        return sb.toString();
    }

    @Tool(name = "fluid_normal_shock",
          description = "Normal shock wave relations for supersonic flow. "
                  + "Given the upstream Mach number M₁ > 1, computes downstream Mach number M₂, "
                  + "pressure ratio, temperature ratio, density ratio, stagnation pressure ratio, "
                  + "and total pressure loss. "
                  + "Use for inlet shock analysis, intake design, and supersonic diffuser calculations.")
    public String normalShock(
            @ToolParam(description = "Upstream Mach number M₁ (must be > 1 for a real shock)") Double M1,
            @ToolParam(description = "Upstream static pressure P₁ in Pa (optional; used to compute absolute values)", required = false) Double P1,
            @ToolParam(description = "Upstream static temperature T₁ in K (optional)", required = false) Double T1,
            @ToolParam(description = "Ratio of specific heats γ (default 1.4 for air)", required = false) Double gamma) {

        if (M1 == null) return "Error: M1 is required.";
        if (M1 <= 1.0) return "Error: Normal shocks only exist for M1 > 1 (supersonic flow). Got M1 = " + fmt(M1) + ".";

        double g = (gamma != null) ? gamma : GAMMA_AIR;
        double g1 = g - 1.0;
        double g2 = g + 1.0;
        double M1sq = M1 * M1;

        // Rankine–Hugoniot relations
        double M2sq = (M1sq * g1 + 2.0) / (2.0 * g * M1sq - g1);
        double M2 = Math.sqrt(M2sq);

        double Pratio   = (2.0 * g * M1sq - g1) / g2;  // P2/P1
        double Tratio   = Pratio * (2.0 + g1 * M1sq) / (g2 * M1sq); // T2/T1 = P2/P1 * (ρ1/ρ2)
        double rhoRatio = g2 * M1sq / (g1 * M1sq + 2.0); // ρ2/ρ1
        double Pratio_0 = Math.pow((g2 * M1sq) / (g1 * M1sq + 2.0), g / g1)
                        * Math.pow(g2 / (2.0 * g * M1sq - g1), 1.0 / g1); // P02/P01

        // Entropy change (normalized): ΔS/R = ln(P01/P02)
        double entropyRise = -Math.log(Pratio_0);

        StringBuilder sb = new StringBuilder();
        sb.append("Normal Shock Wave Relations\n");
        sb.append("───────────────────────────\n\n");
        sb.append("Formula: Rankine–Hugoniot relations\n\n");
        sb.append("UPSTREAM (1):\n");
        sb.append("  M₁ = ").append(fmt(M1)).append("  (supersonic)\n");
        if (P1 != null) sb.append("  P₁ = ").append(fmt(P1)).append(" Pa\n");
        if (T1 != null) sb.append("  T₁ = ").append(fmt(T1)).append(" K\n");
        sb.append("  γ  = ").append(fmt(g)).append("\n\n");
        sb.append("DOWNSTREAM (2):\n");
        sb.append("  M₂ = ").append(fmt(M2)).append("  (subsonic)\n");
        sb.append("  P₂/P₁ = ").append(fmt(Pratio));
        if (P1 != null) sb.append("  → P₂ = ").append(fmt(P1 * Pratio)).append(" Pa");
        sb.append("\n");
        sb.append("  T₂/T₁ = ").append(fmt(Tratio));
        if (T1 != null) sb.append("  → T₂ = ").append(fmt(T1 * Tratio)).append(" K");
        sb.append("\n");
        sb.append("  ρ₂/ρ₁ = ").append(fmt(rhoRatio)).append("\n\n");
        sb.append("STAGNATION QUANTITIES:\n");
        sb.append("  P₀₂/P₀₁ = ").append(fmt(Pratio_0)).append("  (stagnation pressure recovery)\n");
        sb.append("  Loss    = ").append(fmt((1.0 - Pratio_0) * 100)).append("%  stagnation pressure lost\n");
        sb.append("  ΔS/R    = ln(P₀₁/P₀₂) = ").append(fmt(entropyRise)).append("  (entropy rise)\n");
        sb.append("  (T₀ is conserved across a normal shock — stagnation temperature unchanged)\n");

        return sb.toString();
    }

    @Tool(name = "fluid_aerodynamics",
          description = "Compute aerodynamic lift, drag, and related forces on an airfoil or body. "
                  + "L = ½ρv²S·CL,  D = ½ρv²S·CD,  L/D = CL/CD. "
                  + "Also computes dynamic pressure, thrust-to-drag ratio, and glide ratio. "
                  + "Use for wing sizing, performance estimation, and load factor calculations.")
    public String aerodynamics(
            @ToolParam(description = "Freestream velocity in m/s") Double velocity,
            @ToolParam(description = "Air density in kg/m³ (e.g. 1.225 at sea level, use fluid_isa to get altitude value)") Double density,
            @ToolParam(description = "Reference area (wing area) in m²") Double area,
            @ToolParam(description = "Lift coefficient CL (dimensionless)") Double CL,
            @ToolParam(description = "Drag coefficient CD (dimensionless)") Double CD,
            @ToolParam(description = "Aircraft mass in kg (optional; used to compute load factor n = L/(mg))", required = false) Double mass) {

        if (velocity == null || density == null || area == null || CL == null || CD == null) {
            return "Error: velocity, density, area, CL, and CD are all required.";
        }

        double q = 0.5 * density * velocity * velocity; // dynamic pressure
        double L = q * area * CL;
        double D = q * area * CD;
        double LD = CL / CD;
        double g = 9.80665;

        StringBuilder sb = new StringBuilder();
        sb.append("Aerodynamics: Lift & Drag\n");
        sb.append("─────────────────────────\n\n");
        sb.append("GIVEN:\n");
        sb.append("  v   = ").append(fmt(velocity)).append(" m/s\n");
        sb.append("  ρ   = ").append(fmt(density)).append(" kg/m³\n");
        sb.append("  S   = ").append(fmt(area)).append(" m²  (reference area)\n");
        sb.append("  CL  = ").append(fmt(CL)).append("\n");
        sb.append("  CD  = ").append(fmt(CD)).append("\n\n");
        sb.append("RESULTS:\n");
        sb.append("  q   = ½ρv² = ").append(fmt(q)).append(" Pa  (dynamic pressure)\n");
        sb.append("  L   = qS·CL = ").append(fmt(L)).append(" N  (").append(fmt(L / 1000)).append(" kN)\n");
        sb.append("  D   = qS·CD = ").append(fmt(D)).append(" N  (").append(fmt(D / 1000)).append(" kN)\n");
        sb.append("  L/D = CL/CD = ").append(fmt(LD)).append("  (aerodynamic efficiency)\n");
        sb.append("  Glide ratio = ").append(fmt(LD)).append(":1\n");

        if (mass != null) {
            double W = mass * g;
            double n = L / W;
            double stallMargin = (L - W) / W * 100;
            sb.append("\nLOAD FACTOR (mass = ").append(fmt(mass)).append(" kg, W = ").append(fmt(W)).append(" N):\n");
            sb.append("  n = L/W = ").append(fmt(n)).append("  (").append(n >= 1 ? "lift > weight" : "insufficient lift").append(")\n");
            sb.append("  Excess lift = ").append(fmt(stallMargin)).append("%\n");
            sb.append("  Thrust needed to maintain level flight ≥ D = ").append(fmt(D)).append(" N\n");
        }

        return sb.toString();
    }

    @Tool(name = "fluid_pipe_flow",
          description = "Pipe and duct flow calculations using the Darcy-Weisbach equation. "
                  + "Computes head loss, pressure drop, and friction factor. "
                  + "Supports minor losses (fittings), flow velocity from flow rate, "
                  + "and estimates friction factor for both laminar and turbulent flow (Colebrook–White / Swamee–Jain). "
                  + "Use for hydraulic system design, fuel lines, and pneumatic ducts.")
    public String pipeFlow(
            @ToolParam(description = "Pipe/duct inner diameter in m") Double diameter,
            @ToolParam(description = "Pipe/duct length in m") Double length,
            @ToolParam(description = "Average flow velocity in m/s (provide either this OR volumeFlowRate)") Double velocity,
            @ToolParam(description = "Volumetric flow rate in m³/s (used if velocity is null)", required = false) Double volumeFlowRate,
            @ToolParam(description = "Fluid density in kg/m³") Double density,
            @ToolParam(description = "Fluid kinematic viscosity in m²/s "
                    + "(default 1.461×10⁻⁵ for air at sea level, 1.004×10⁻⁶ for water at 20°C)", required = false) Double kinematicViscosity,
            @ToolParam(description = "Pipe wall absolute roughness ε in m "
                    + "(0 for smooth pipe; steel ≈ 4.6×10⁻⁵, PVC ≈ 1.5×10⁻⁶)", required = false) Double roughness,
            @ToolParam(description = "Sum of minor loss coefficients ΣK (e.g. 0.5 for sharp entrance + 1.0 exit = 1.5). "
                    + "Use 0 if only computing major (friction) losses.", required = false) Double minorLossK) {

        if (diameter == null || length == null || density == null) {
            return "Error: diameter, length, and density are required.";
        }
        if (velocity == null && volumeFlowRate == null) {
            return "Error: provide either velocity or volumeFlowRate.";
        }

        double nu = (kinematicViscosity != null) ? kinematicViscosity : MU_AIR_SL / RHO_AIR_SL;
        double eps = (roughness != null) ? roughness : 0.0;
        double K   = (minorLossK != null) ? minorLossK : 0.0;
        double g   = 9.80665;
        double A   = Math.PI * diameter * diameter / 4.0;

        double v;
        if (velocity != null) {
            v = velocity;
        } else {
            v = volumeFlowRate / A;
        }

        double Q = v * A; // flow rate
        double Re = v * diameter / nu;

        // Friction factor
        double f;
        String fMethod;
        if (Re < 2300) {
            f = 64.0 / Re;
            fMethod = "Laminar: f = 64/Re";
        } else {
            // Swamee–Jain explicit approximation of Colebrook–White
            double relRough = eps / diameter;
            if (relRough == 0 && Re > 0) {
                // Smooth pipe: Blasius
                f = 0.316 * Math.pow(Re, -0.25);
                fMethod = "Turbulent smooth (Blasius): f = 0.316·Re^(-0.25)";
            } else {
                double logArg = relRough / 3.7 + 5.74 / Math.pow(Re, 0.9);
                f = 0.25 / Math.pow(Math.log10(logArg), 2);
                fMethod = "Turbulent rough (Swamee-Jain)";
            }
        }

        double hf_major = f * (length / diameter) * (v * v) / (2.0 * g);   // major head loss (m)
        double hf_minor = K * v * v / (2.0 * g);                             // minor head loss (m)
        double hf_total = hf_major + hf_minor;
        double dP_major = density * g * hf_major;                             // pressure drop (Pa)
        double dP_minor = density * g * hf_minor;
        double dP_total = density * g * hf_total;

        StringBuilder sb = new StringBuilder();
        sb.append("Pipe Flow (Darcy-Weisbach)\n");
        sb.append("──────────────────────────\n\n");
        sb.append("GIVEN:\n");
        sb.append("  D = ").append(fmt(diameter * 1000)).append(" mm,  L = ").append(fmt(length)).append(" m\n");
        sb.append("  ρ = ").append(fmt(density)).append(" kg/m³,  ν = ").append(sci(nu)).append(" m²/s\n");
        sb.append("  ε = ").append(sci(eps)).append(" m  (roughness),  ε/D = ").append(sci(eps / diameter)).append("\n");
        if (K > 0) sb.append("  ΣK = ").append(fmt(K)).append("  (minor loss coefficients)\n");
        sb.append("\nFLOW:\n");
        sb.append("  A  = π/4·D² = ").append(sci(A)).append(" m²\n");
        sb.append("  v  = ").append(fmt(v)).append(" m/s\n");
        sb.append("  Q  = v·A    = ").append(sci(Q)).append(" m³/s  = ").append(fmt(Q * 1000)).append(" L/s\n");
        sb.append("  Re = v·D/ν  = ").append(fmt(Re)).append("\n\n");
        sb.append("FRICTION FACTOR:\n");
        sb.append("  f = ").append(fmt(f)).append("  (").append(fMethod).append(")\n\n");
        sb.append("LOSSES:\n");
        sb.append("  Major (friction): hf = f·(L/D)·v²/2g = ").append(fmt(hf_major)).append(" m  → ΔP = ").append(fmt(dP_major)).append(" Pa\n");
        if (K > 0) {
            sb.append("  Minor (fittings): hm = ΣK·v²/2g    = ").append(fmt(hf_minor)).append(" m  → ΔP = ").append(fmt(dP_minor)).append(" Pa\n");
        }
        sb.append("  TOTAL:            ht             = ").append(fmt(hf_total)).append(" m  → ΔP = ").append(fmt(dP_total)).append(" Pa  (")
          .append(fmt(dP_total / 1000)).append(" kPa)\n");

        return sb.toString();
    }

    @Tool(name = "fluid_continuity",
          description = "Apply the continuity equation (mass conservation) between two cross-sections of a duct or pipe. "
                  + "For incompressible flow: A₁v₁ = A₂v₂. "
                  + "For compressible flow: ρ₁A₁v₁ = ρ₂A₂v₂. "
                  + "Solves for any one unknown (A, v, or ρ) given the others. "
                  + "Use for nozzle throat sizing, flow area changes, and mass flow calculations.")
    public String continuity(
            @ToolParam(description = "Area at section 1 in m²") Double A1,
            @ToolParam(description = "Velocity at section 1 in m/s") Double v1,
            @ToolParam(description = "Density at section 1 in kg/m³ (optional; omit for incompressible flow)", required = false) Double rho1,
            @ToolParam(description = "Area at section 2 in m² (null to solve for it)", required = false) Double A2,
            @ToolParam(description = "Velocity at section 2 in m/s (null to solve for it)", required = false) Double v2,
            @ToolParam(description = "Density at section 2 in kg/m³ (optional; omit for incompressible flow)", required = false) Double rho2) {

        if (A1 == null || v1 == null) return "Error: A1 and v1 are required.";

        boolean compressible = rho1 != null;
        double r1 = compressible ? rho1 : 1.0;
        double r2 = (rho2 != null) ? rho2 : r1;

        double massFlow = r1 * A1 * v1;

        StringBuilder sb = new StringBuilder();
        sb.append("Continuity Equation (Mass Conservation)\n");
        sb.append("───────────────────────────────────────\n\n");
        sb.append(compressible ? "Formula: ρ₁A₁v₁ = ρ₂A₂v₂  (compressible)\n\n"
                               : "Formula: A₁v₁ = A₂v₂  (incompressible, ρ = const)\n\n");
        sb.append("SECTION 1:\n");
        sb.append("  A₁ = ").append(fmt(A1)).append(" m²\n");
        sb.append("  v₁ = ").append(fmt(v1)).append(" m/s\n");
        if (compressible) sb.append("  ρ₁ = ").append(fmt(rho1)).append(" kg/m³\n");
        sb.append("  Q₁ = A₁·v₁ = ").append(fmt(A1 * v1)).append(" m³/s\n");
        if (compressible) sb.append("  ṁ  = ρ₁A₁v₁ = ").append(fmt(massFlow)).append(" kg/s\n");
        sb.append("\n");

        if (A2 == null && v2 != null) {
            double A2solved = massFlow / (r2 * v2);
            double d2 = Math.sqrt(4.0 * A2solved / Math.PI);
            sb.append("SECTION 2 (solving for A₂):\n");
            sb.append("  v₂ = ").append(fmt(v2)).append(" m/s  (given)\n");
            if (compressible) sb.append("  ρ₂ = ").append(fmt(r2)).append(" kg/m³\n");
            sb.append("  A₂ = ").append(fmt(A2solved)).append(" m²  (solved)\n");
            sb.append("  d₂ = √(4A₂/π) = ").append(fmt(d2 * 1000)).append(" mm  (equivalent circular diameter)\n");
            sb.append("  Area ratio A₂/A₁ = ").append(fmt(A2solved / A1)).append("\n");
        } else if (v2 == null && A2 != null) {
            double v2solved = massFlow / (r2 * A2);
            sb.append("SECTION 2 (solving for v₂):\n");
            sb.append("  A₂ = ").append(fmt(A2)).append(" m²  (given)\n");
            if (compressible) sb.append("  ρ₂ = ").append(fmt(r2)).append(" kg/m³\n");
            sb.append("  v₂ = ").append(fmt(v2solved)).append(" m/s  (solved)\n");
            sb.append("  Area ratio A₂/A₁ = ").append(fmt(A2 / A1)).append("\n");
            sb.append("  Velocity ratio v₂/v₁ = ").append(fmt(v2solved / v1)).append("\n");
        } else if (A2 != null && v2 != null) {
            double massFlow2 = r2 * A2 * v2;
            sb.append("SECTION 2 (verification):\n");
            sb.append("  A₂ = ").append(fmt(A2)).append(" m²\n");
            sb.append("  v₂ = ").append(fmt(v2)).append(" m/s\n");
            if (compressible) sb.append("  ρ₂ = ").append(fmt(r2)).append(" kg/m³\n");
            sb.append("  ṁ₂ = ").append(fmt(massFlow2)).append(" kg/s\n");
            sb.append("  ṁ₁ = ").append(fmt(massFlow)).append(" kg/s\n");
            double err = Math.abs(massFlow - massFlow2) / massFlow * 100;
            sb.append("  Mass balance error: ").append(fmt(err)).append("%\n");
        } else {
            sb.append("  Provide either A2 or v2 (as the unknown) to solve for the other.\n");
        }

        return sb.toString();
    }

    @Tool(name = "fluid_isa_atmosphere",
          description = "International Standard Atmosphere (ISA) model. "
                  + "Given altitude in meters, computes temperature, pressure, density, "
                  + "speed of sound, and kinematic viscosity. "
                  + "Valid from sea level to 86 km. Covers troposphere (0–11 km) and stratosphere (11–20 km). "
                  + "Use to get air properties at cruise altitude for aerodynamic calculations.")
    public String isaAtmosphere(
            @ToolParam(description = "Geometric altitude in meters (0 = sea level, e.g. 10000 for 10 km)") Double altitude,
            @ToolParam(description = "ISA offset in Kelvin (ISA+X deviation, e.g. 15 for ISA+15). Default 0.", required = false) Double isaOffset) {

        if (altitude == null) return "Error: altitude is required.";
        if (altitude < 0 || altitude > 86000) return "Error: altitude must be between 0 and 86000 m.";

        double dT = (isaOffset != null) ? isaOffset : 0.0;

        // ISA layer definitions: [base alt (m), base T (K), lapse rate (K/m), base P (Pa)]
        double T, P;
        double h = altitude;

        if (h <= 11000) {
            // Troposphere: T = 288.15 - 0.0065h
            double lapseRate = 0.0065;
            T = T_SL - lapseRate * h;
            P = P_SL * Math.pow(T / T_SL, 5.2561); // P/P0 = (T/T0)^(g/RL)
        } else if (h <= 20000) {
            // Lower stratosphere: isothermal at 216.65 K
            double T11 = 216.65;
            double P11 = 22632.1;
            T = T11;
            P = P11 * Math.exp(-0.0001576884 * (h - 11000));
        } else if (h <= 32000) {
            // Upper stratosphere: T rises at 0.001 K/m
            double T20 = 216.65;
            double P20 = 5474.89;
            T = T20 + 0.001 * (h - 20000);
            P = P20 * Math.pow(T / T20, -34.1632);
        } else {
            // Simplified upper layers (35–86 km) — less common in aerospace design
            T = 228.65 + 0.0028 * (h - 32000);
            P = 868.019 * Math.exp(-0.0000124578 * (h - 32000));
        }

        T += dT; // apply ISA offset

        double rho = P / (R_AIR * T);
        double a = Math.sqrt(GAMMA_AIR * R_AIR * T);

        // Sutherland's law for dynamic viscosity
        double mu = 1.458e-6 * Math.pow(T, 1.5) / (T + 110.4);
        double nu = mu / rho;

        double T_C = T - 273.15;
        double P_hPa = P / 100.0;
        double rho_rel = rho / RHO_AIR_SL;

        StringBuilder sb = new StringBuilder();
        sb.append("ISA Atmosphere at ").append(fmt(altitude / 1000.0)).append(" km");
        if (dT != 0) sb.append(" (ISA").append(dT > 0 ? "+" : "").append(fmt(dT)).append(")");
        sb.append("\n");
        sb.append("─".repeat(40)).append("\n\n");
        sb.append("  Altitude       = ").append(fmt(altitude)).append(" m  = ").append(fmt(altitude / 1000.0)).append(" km\n");
        sb.append("  Temperature    = ").append(fmt(T)).append(" K  = ").append(fmt(T_C)).append(" °C\n");
        sb.append("  Pressure       = ").append(fmt(P)).append(" Pa  = ").append(fmt(P_hPa)).append(" hPa\n");
        sb.append("  Density        = ").append(fmt(rho)).append(" kg/m³  (σ = ρ/ρ₀ = ").append(fmt(rho_rel)).append(")\n");
        sb.append("  Speed of sound = ").append(fmt(a)).append(" m/s\n");
        sb.append("  Dyn. viscosity = ").append(sci(mu)).append(" Pa·s  (Sutherland)\n");
        sb.append("  Kin. viscosity = ").append(sci(nu)).append(" m²/s\n\n");
        sb.append("  P/P_SL = ").append(fmt(P / P_SL)).append("\n");
        sb.append("  T/T_SL = ").append(fmt(T / (T_SL + dT))).append("\n");
        sb.append("  ρ/ρ_SL = ").append(fmt(rho_rel)).append("\n");

        return sb.toString();
    }

    // ── Helpers ──

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
