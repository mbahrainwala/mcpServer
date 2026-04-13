package com.behrainwala.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MCP tool for advanced mathematics: equation solving, geometry, algebra,
 * statistics, trigonometry, and common formulas.
 */
@Service
public class AdvancedMathTool {

    private static final MathContext MC = new MathContext(15);

    // ────────────────────────────────────────────────────────────────────────
    // ALGEBRA
    // ────────────────────────────────────────────────────────────────────────

    @Tool(name = "solve_quadratic", description = "Solve a quadratic equation ax² + bx + c = 0. "
            + "Returns both roots (real or complex). Use this for any second-degree polynomial equation.")
    public String solveQuadratic(
            @ToolParam(description = "Coefficient a (the x² coefficient, must not be 0)") double a,
            @ToolParam(description = "Coefficient b (the x coefficient)") double b,
            @ToolParam(description = "Coefficient c (the constant term)") double c) {

        if (a == 0) {
            if (b == 0) {
                return c == 0 ? "Identity: 0 = 0 (infinite solutions)" : "No solution: " + fmt(c) + " ≠ 0";
            }
            double x = -c / b;
            return format("Linear Equation", fmt(b) + "x + " + fmt(c) + " = 0",
                    "x = " + fmt(x));
        }

        double discriminant = b * b - 4 * a * c;

        StringBuilder sb = new StringBuilder();
        sb.append("Quadratic Equation\n");
        sb.append("──────────────────\n");
        sb.append("Equation: ").append(formatQuadratic(a, b, c)).append(" = 0\n\n");
        sb.append("Using the quadratic formula: x = (-b ± √(b²-4ac)) / 2a\n\n");
        sb.append("Discriminant (b²-4ac): ").append(fmt(discriminant)).append("\n\n");

        if (discriminant > 0) {
            double sqrtD = Math.sqrt(discriminant);
            double x1 = (-b + sqrtD) / (2 * a);
            double x2 = (-b - sqrtD) / (2 * a);
            sb.append("Two distinct real roots:\n");
            sb.append("  x₁ = ").append(fmt(x1)).append("\n");
            sb.append("  x₂ = ").append(fmt(x2)).append("\n");
            sb.append("\nVerification:\n");
            sb.append("  f(x₁) = ").append(fmt(a * x1 * x1 + b * x1 + c)).append("\n");
            sb.append("  f(x₂) = ").append(fmt(a * x2 * x2 + b * x2 + c));
        } else if (discriminant == 0) {
            double x = -b / (2 * a);
            sb.append("One repeated real root:\n");
            sb.append("  x = ").append(fmt(x));
        } else {
            double realPart = -b / (2 * a);
            double imagPart = Math.sqrt(-discriminant) / (2 * a);
            sb.append("Two complex conjugate roots:\n");
            sb.append("  x₁ = ").append(fmt(realPart)).append(" + ").append(fmt(imagPart)).append("i\n");
            sb.append("  x₂ = ").append(fmt(realPart)).append(" - ").append(fmt(imagPart)).append("i");
        }

        return sb.toString();
    }

    @Tool(name = "solve_linear_system", description = "Solve a system of two linear equations: "
            + "a1*x + b1*y = c1 and a2*x + b2*y = c2. Returns the values of x and y.")
    public String solveLinearSystem(
            @ToolParam(description = "Coefficient a1 in first equation (a1*x + b1*y = c1)") double a1,
            @ToolParam(description = "Coefficient b1 in first equation") double b1,
            @ToolParam(description = "Constant c1 in first equation") double c1,
            @ToolParam(description = "Coefficient a2 in second equation (a2*x + b2*y = c2)") double a2,
            @ToolParam(description = "Coefficient b2 in second equation") double b2,
            @ToolParam(description = "Constant c2 in second equation") double c2) {

        double det = a1 * b2 - a2 * b1;

        StringBuilder sb = new StringBuilder();
        sb.append("System of Linear Equations\n");
        sb.append("─────────────────────────\n");
        sb.append("  ").append(formatLinear(a1, b1, c1)).append("\n");
        sb.append("  ").append(formatLinear(a2, b2, c2)).append("\n\n");
        sb.append("Determinant: ").append(fmt(det)).append("\n\n");

        if (Math.abs(det) < 1e-12) {
            boolean consistent = Math.abs(a1 * c2 - a2 * c1) < 1e-12
                    && Math.abs(b1 * c2 - b2 * c1) < 1e-12;
            sb.append(consistent
                    ? "Infinite solutions (dependent equations — same line)"
                    : "No solution (parallel lines — inconsistent system)");
        } else {
            double x = (c1 * b2 - c2 * b1) / det;
            double y = (a1 * c2 - a2 * c1) / det;
            sb.append("Solution (Cramer's Rule):\n");
            sb.append("  x = ").append(fmt(x)).append("\n");
            sb.append("  y = ").append(fmt(y)).append("\n\n");
            sb.append("Verification:\n");
            sb.append("  Eq1: ").append(fmt(a1)).append("(").append(fmt(x)).append(") + ")
                    .append(fmt(b1)).append("(").append(fmt(y)).append(") = ")
                    .append(fmt(a1 * x + b1 * y)).append(" ✓\n");
            sb.append("  Eq2: ").append(fmt(a2)).append("(").append(fmt(x)).append(") + ")
                    .append(fmt(b2)).append("(").append(fmt(y)).append(") = ")
                    .append(fmt(a2 * x + b2 * y)).append(" ✓");
        }

        return sb.toString();
    }

    @Tool(name = "compute_polynomial", description = "Evaluate a polynomial at a given value of x. "
            + "Provide coefficients from highest degree to constant. "
            + "For example, for 3x³ - 2x + 5, provide coefficients [3, 0, -2, 5].")
    public String computePolynomial(
            @ToolParam(description = "Coefficients from highest degree to constant, as comma-separated values. "
                    + "Example: '3,0,-2,5' for 3x³ - 2x + 5") String coefficients,
            @ToolParam(description = "The value of x to evaluate at") double x) {

        try {
            String[] parts = coefficients.split(",");
            double[] coeffs = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                coeffs[i] = Double.parseDouble(parts[i].strip());
            }

            int degree = coeffs.length - 1;

            // Horner's method for evaluation
            double result = coeffs[0];
            for (int i = 1; i < coeffs.length; i++) {
                result = result * x + coeffs[i];
            }

            return "Polynomial Evaluation\n" +
                    "────────────────────\n" +
                    "P(x) = " + formatPolynomial(coeffs) + "\n\n" +
                    "P(" + fmt(x) + ") = " + fmt(result);

        } catch (NumberFormatException e) {
            return "Error: Invalid coefficients. Provide comma-separated numbers like '3,0,-2,5'";
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GEOMETRY
    // ────────────────────────────────────────────────────────────────────────

    @Tool(name = "triangle_properties", description = "Calculate all properties of a triangle given its three sides "
            + "(SSS), two sides and included angle (SAS), or three coordinates. "
            + "Returns: area, perimeter, angles, heights, circumradius, inradius, and type.")
    public String triangleProperties(
            @ToolParam(description = "Side a length") double a,
            @ToolParam(description = "Side b length") double b,
            @ToolParam(description = "Side c length") double c) {

        // Validate triangle inequality
        if (a <= 0 || b <= 0 || c <= 0) {
            return "Error: All sides must be positive numbers.";
        }
        if (a + b <= c || a + c <= b || b + c <= a) {
            return "Error: These sides do not form a valid triangle (violates triangle inequality).";
        }

        double perimeter = a + b + c;
        double s = perimeter / 2; // semi-perimeter

        // Heron's formula
        double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));

        // Angles using law of cosines
        double angleA = Math.toDegrees(Math.acos((b * b + c * c - a * a) / (2 * b * c)));
        double angleB = Math.toDegrees(Math.acos((a * a + c * c - b * b) / (2 * a * c)));
        double angleC = 180 - angleA - angleB;

        // Heights
        double ha = 2 * area / a;
        double hb = 2 * area / b;
        double hc = 2 * area / c;

        // Circumradius and inradius
        double R = (a * b * c) / (4 * area);
        double r = area / s;

        // Medians
        double ma = 0.5 * Math.sqrt(2 * b * b + 2 * c * c - a * a);
        double mb = 0.5 * Math.sqrt(2 * a * a + 2 * c * c - b * b);
        double mc = 0.5 * Math.sqrt(2 * a * a + 2 * b * b - c * c);

        // Classify
        String type = classifyTriangle(a, b, c, angleA, angleB, angleC);

        return "Triangle Properties\n" +
                "───────────────────\n" +
                "Sides: a = " + fmt(a) + ", b = " + fmt(b) + ", c = " + fmt(c) + "\n" +
                "Type: " + type + "\n\n" +
                "MEASUREMENTS\n" +
                "  Perimeter: " + fmt(perimeter) + "\n" +
                "  Area: " + fmt(area) + " (Heron's formula)\n" +
                "  Semi-perimeter: " + fmt(s) + "\n\n" +
                "ANGLES\n" +
                "  A (opposite a): " + fmt(angleA) + "°\n" +
                "  B (opposite b): " + fmt(angleB) + "°\n" +
                "  C (opposite c): " + fmt(angleC) + "°\n" +
                "  Sum: " + fmt(angleA + angleB + angleC) + "° ✓\n\n" +
                "HEIGHTS (ALTITUDES)\n" +
                "  hₐ = " + fmt(ha) + "\n" +
                "  hᵦ = " + fmt(hb) + "\n" +
                "  h꜀ = " + fmt(hc) + "\n\n" +
                "MEDIANS\n" +
                "  mₐ = " + fmt(ma) + "\n" +
                "  mᵦ = " + fmt(mb) + "\n" +
                "  m꜀ = " + fmt(mc) + "\n\n" +
                "RADII\n" +
                "  Circumradius (R): " + fmt(R) + "\n" +
                "  Inradius (r): " + fmt(r);
    }

    @Tool(name = "triangle_area", description = "Calculate the area of a triangle using various methods: "
            + "base & height, two sides & included angle, three vertices (coordinates), or Heron's formula (three sides). "
            + "Specify the method parameter to choose which formula to use.")
    public String triangleArea(
            @ToolParam(description = "Method: 'base_height', 'two_sides_angle', 'coordinates', or 'herons'") String method,
            @ToolParam(description = "Comma-separated values depending on method:\n"
                    + "  base_height: base,height (e.g. '10,5')\n"
                    + "  two_sides_angle: sideA,sideB,angleDegrees (e.g. '5,7,30')\n"
                    + "  coordinates: x1,y1,x2,y2,x3,y3 (e.g. '0,0,4,0,2,3')\n"
                    + "  herons: sideA,sideB,sideC (e.g. '3,4,5')") String values) {

        try {
            double[] v = parseValues(values);

            return switch (method.strip().toLowerCase()) {
                case "base_height" -> {
                    if (v.length < 2) throw new IllegalArgumentException("Need base and height");
                    double area = 0.5 * v[0] * v[1];
                    yield format("Triangle Area (Base × Height)",
                            "base = " + fmt(v[0]) + ", height = " + fmt(v[1]),
                            "Area = ½ × " + fmt(v[0]) + " × " + fmt(v[1]) + " = " + fmt(area));
                }
                case "two_sides_angle" -> {
                    if (v.length < 3) throw new IllegalArgumentException("Need sideA, sideB, and angle");
                    double angleRad = Math.toRadians(v[2]);
                    double area = 0.5 * v[0] * v[1] * Math.sin(angleRad);
                    yield format("Triangle Area (Two Sides + Included Angle)",
                            "a = " + fmt(v[0]) + ", b = " + fmt(v[1]) + ", θ = " + fmt(v[2]) + "°",
                            "Area = ½ × a × b × sin(θ) = " + fmt(area));
                }
                case "coordinates" -> {
                    if (v.length < 6) throw new IllegalArgumentException("Need x1,y1,x2,y2,x3,y3");
                    double area = 0.5 * Math.abs(
                            v[0] * (v[3] - v[5]) + v[2] * (v[5] - v[1]) + v[4] * (v[1] - v[3]));
                    yield format("Triangle Area (Coordinates — Shoelace Formula)",
                            "A(" + fmt(v[0]) + "," + fmt(v[1]) + "), B(" + fmt(v[2]) + "," + fmt(v[3])
                                    + "), C(" + fmt(v[4]) + "," + fmt(v[5]) + ")",
                            "Area = " + fmt(area));
                }
                case "herons" -> {
                    if (v.length < 3) throw new IllegalArgumentException("Need sideA, sideB, sideC");
                    double s = (v[0] + v[1] + v[2]) / 2;
                    double area = Math.sqrt(s * (s - v[0]) * (s - v[1]) * (s - v[2]));
                    yield format("Triangle Area (Heron's Formula)",
                            "a = " + fmt(v[0]) + ", b = " + fmt(v[1]) + ", c = " + fmt(v[2])
                                    + ", s = " + fmt(s),
                            "Area = √(s(s-a)(s-b)(s-c)) = " + fmt(area));
                }
                default -> "Error: Unknown method '" + method + "'. Use: base_height, two_sides_angle, coordinates, or herons.";
            };

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "circle_properties", description = "Calculate all properties of a circle given its radius, "
            + "diameter, area, or circumference. Provide any one measurement and get all others.")
    public String circleProperties(
            @ToolParam(description = "The known measurement type: 'radius', 'diameter', 'area', or 'circumference'") String knownType,
            @ToolParam(description = "The known measurement value") double value) {

        if (value <= 0) return "Error: Value must be positive.";

        double radius = switch (knownType.strip().toLowerCase()) {
            case "radius", "r" -> value;
            case "diameter", "d" -> value / 2;
            case "area", "a" -> Math.sqrt(value / Math.PI);
            case "circumference", "c" -> value / (2 * Math.PI);
            default -> -1;
        };

        if (radius < 0) {
            return "Error: Unknown type '" + knownType + "'. Use: radius, diameter, area, or circumference.";
        }

        double diameter = 2 * radius;
        double area = Math.PI * radius * radius;
        double circumference = 2 * Math.PI * radius;
        double sectorAreaPerDeg = area / 360;

        StringBuilder sb = new StringBuilder();
        sb.append("Circle Properties\n");
        sb.append("─────────────────\n");
        sb.append("Given: ").append(knownType).append(" = ").append(fmt(value)).append("\n\n");
        sb.append("  Radius (r):        ").append(fmt(radius)).append("\n");
        sb.append("  Diameter (d):      ").append(fmt(diameter)).append("\n");
        sb.append("  Circumference (C): ").append(fmt(circumference)).append("\n");
        sb.append("  Area (A):          ").append(fmt(area)).append("\n\n");
        sb.append("Formulas used:\n");
        sb.append("  C = 2πr = ").append(fmt(circumference)).append("\n");
        sb.append("  A = πr² = ").append(fmt(area)).append("\n");
        sb.append("  Sector area per degree: ").append(fmt(sectorAreaPerDeg));

        return sb.toString();
    }

    @Tool(name = "shape_area_volume", description = "Calculate the area and/or volume of common geometric shapes. "
            + "Supported shapes: rectangle, parallelogram, trapezoid, ellipse, "
            + "sphere, cylinder, cone, cube, rectangular_prism, pyramid.")
    public String shapeAreaVolume(
            @ToolParam(description = "Shape name: rectangle, parallelogram, trapezoid, ellipse, "
                    + "sphere, cylinder, cone, cube, rectangular_prism, pyramid") String shape,
            @ToolParam(description = "Comma-separated dimensions depending on shape:\n"
                    + "  rectangle: length,width\n"
                    + "  parallelogram: base,height\n"
                    + "  trapezoid: base1,base2,height\n"
                    + "  ellipse: semi_major_axis,semi_minor_axis\n"
                    + "  sphere: radius\n"
                    + "  cylinder: radius,height\n"
                    + "  cone: radius,height\n"
                    + "  cube: side\n"
                    + "  rectangular_prism: length,width,height\n"
                    + "  pyramid: base_length,base_width,height") String dimensions) {

        try {
            double[] d = parseValues(dimensions);

            return switch (shape.strip().toLowerCase().replace(" ", "_")) {
                case "rectangle" -> {
                    checkArgs(d, 2, "length, width");
                    double area = d[0] * d[1];
                    double perimeter = 2 * (d[0] + d[1]);
                    double diagonal = Math.sqrt(d[0] * d[0] + d[1] * d[1]);
                    yield formatShape("Rectangle", "l=" + fmt(d[0]) + ", w=" + fmt(d[1]),
                            "Area = l × w = " + fmt(area),
                            "Perimeter = 2(l+w) = " + fmt(perimeter),
                            "Diagonal = √(l²+w²) = " + fmt(diagonal));
                }
                case "parallelogram" -> {
                    checkArgs(d, 2, "base, height");
                    double area = d[0] * d[1];
                    yield formatShape("Parallelogram", "b=" + fmt(d[0]) + ", h=" + fmt(d[1]),
                            "Area = b × h = " + fmt(area), null, null);
                }
                case "trapezoid" -> {
                    checkArgs(d, 3, "base1, base2, height");
                    double area = 0.5 * (d[0] + d[1]) * d[2];
                    yield formatShape("Trapezoid", "a=" + fmt(d[0]) + ", b=" + fmt(d[1]) + ", h=" + fmt(d[2]),
                            "Area = ½(a+b)×h = " + fmt(area), null, null);
                }
                case "ellipse" -> {
                    checkArgs(d, 2, "semi_major_axis, semi_minor_axis");
                    double area = Math.PI * d[0] * d[1];
                    double approxPerimeter = Math.PI * (3 * (d[0] + d[1]) - Math.sqrt((3 * d[0] + d[1]) * (d[0] + 3 * d[1])));
                    yield formatShape("Ellipse", "a=" + fmt(d[0]) + ", b=" + fmt(d[1]),
                            "Area = πab = " + fmt(area),
                            "Approx perimeter (Ramanujan) ≈ " + fmt(approxPerimeter), null);
                }
                case "sphere" -> {
                    checkArgs(d, 1, "radius");
                    double sa = 4 * Math.PI * d[0] * d[0];
                    double vol = (4.0 / 3.0) * Math.PI * d[0] * d[0] * d[0];
                    yield formatShape("Sphere", "r=" + fmt(d[0]),
                            "Surface area = 4πr² = " + fmt(sa),
                            "Volume = ⁴⁄₃πr³ = " + fmt(vol), null);
                }
                case "cylinder" -> {
                    checkArgs(d, 2, "radius, height");
                    double lateralSA = 2 * Math.PI * d[0] * d[1];
                    double totalSA = lateralSA + 2 * Math.PI * d[0] * d[0];
                    double vol = Math.PI * d[0] * d[0] * d[1];
                    yield formatShape("Cylinder", "r=" + fmt(d[0]) + ", h=" + fmt(d[1]),
                            "Lateral surface area = 2πrh = " + fmt(lateralSA),
                            "Total surface area = 2πr(r+h) = " + fmt(totalSA),
                            "Volume = πr²h = " + fmt(vol));
                }
                case "cone" -> {
                    checkArgs(d, 2, "radius, height");
                    double slant = Math.sqrt(d[0] * d[0] + d[1] * d[1]);
                    double lateralSA = Math.PI * d[0] * slant;
                    double totalSA = lateralSA + Math.PI * d[0] * d[0];
                    double vol = (1.0 / 3.0) * Math.PI * d[0] * d[0] * d[1];
                    yield formatShape("Cone", "r=" + fmt(d[0]) + ", h=" + fmt(d[1]),
                            "Slant height = √(r²+h²) = " + fmt(slant),
                            "Total surface area = πr(r+l) = " + fmt(totalSA),
                            "Volume = ⅓πr²h = " + fmt(vol));
                }
                case "cube" -> {
                    checkArgs(d, 1, "side");
                    double sa = 6 * d[0] * d[0];
                    double vol = d[0] * d[0] * d[0];
                    double diagonal = d[0] * Math.sqrt(3);
                    yield formatShape("Cube", "s=" + fmt(d[0]),
                            "Surface area = 6s² = " + fmt(sa),
                            "Volume = s³ = " + fmt(vol),
                            "Space diagonal = s√3 = " + fmt(diagonal));
                }
                case "rectangular_prism", "cuboid", "box" -> {
                    checkArgs(d, 3, "length, width, height");
                    double sa = 2 * (d[0] * d[1] + d[1] * d[2] + d[0] * d[2]);
                    double vol = d[0] * d[1] * d[2];
                    double diagonal = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                    yield formatShape("Rectangular Prism", "l=" + fmt(d[0]) + ", w=" + fmt(d[1]) + ", h=" + fmt(d[2]),
                            "Surface area = 2(lw+wh+lh) = " + fmt(sa),
                            "Volume = lwh = " + fmt(vol),
                            "Space diagonal = " + fmt(diagonal));
                }
                case "pyramid" -> {
                    checkArgs(d, 3, "base_length, base_width, height");
                    double baseArea = d[0] * d[1];
                    double vol = (1.0 / 3.0) * baseArea * d[2];
                    double slantL = Math.sqrt((d[1] / 2) * (d[1] / 2) + d[2] * d[2]);
                    double slantW = Math.sqrt((d[0] / 2) * (d[0] / 2) + d[2] * d[2]);
                    double lateralSA = d[0] * slantL + d[1] * slantW;
                    yield formatShape("Rectangular Pyramid", "l=" + fmt(d[0]) + ", w=" + fmt(d[1]) + ", h=" + fmt(d[2]),
                            "Base area = lw = " + fmt(baseArea),
                            "Lateral surface area ≈ " + fmt(lateralSA),
                            "Volume = ⅓lwh = " + fmt(vol));
                }
                default -> "Error: Unknown shape '" + shape + "'. Supported: rectangle, parallelogram, "
                        + "trapezoid, ellipse, sphere, cylinder, cone, cube, rectangular_prism, pyramid.";
            };

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // STATISTICS
    // ────────────────────────────────────────────────────────────────────────

    @Tool(name = "statistics", description = "Calculate descriptive statistics for a dataset. "
            + "Returns: mean, median, mode, standard deviation, variance, min, max, range, "
            + "quartiles, and IQR.")
    public String statistics(
            @ToolParam(description = "Comma-separated list of numbers (e.g. '10,20,30,40,50')") String data) {

        try {
            double[] values = parseValues(data);
            if (values.length == 0) return "Error: No data provided.";

            java.util.Arrays.sort(values);
            int n = values.length;

            // Mean
            double sum = 0;
            for (double v : values) sum += v;
            double mean = sum / n;

            // Median
            double median = (n % 2 == 0)
                    ? (values[n / 2 - 1] + values[n / 2]) / 2.0
                    : values[n / 2];

            // Mode
            String mode = computeMode(values);

            // Variance and Std Dev (population)
            double variance = 0;
            for (double v : values) variance += (v - mean) * (v - mean);
            double populationVariance = variance / n;
            double sampleVariance = n > 1 ? variance / (n - 1) : 0;
            double populationStdDev = Math.sqrt(populationVariance);
            double sampleStdDev = Math.sqrt(sampleVariance);

            // Quartiles
            double q1 = percentile(values, 25);
            double q3 = percentile(values, 75);
            double iqr = q3 - q1;

            // Sum of squares
            double sumOfSquares = 0;
            for (double v : values) sumOfSquares += v * v;

            StringBuilder sb = new StringBuilder();
            sb.append("Descriptive Statistics\n");
            sb.append("─────────────────────\n");
            sb.append("Data points (n): ").append(n).append("\n");
            sb.append("Sorted: ").append(formatArray(values)).append("\n\n");

            sb.append("CENTRAL TENDENCY\n");
            sb.append("  Mean:   ").append(fmt(mean)).append("\n");
            sb.append("  Median: ").append(fmt(median)).append("\n");
            sb.append("  Mode:   ").append(mode).append("\n\n");

            sb.append("DISPERSION\n");
            sb.append("  Min: ").append(fmt(values[0])).append("\n");
            sb.append("  Max: ").append(fmt(values[n - 1])).append("\n");
            sb.append("  Range: ").append(fmt(values[n - 1] - values[0])).append("\n");
            sb.append("  Population variance (σ²): ").append(fmt(populationVariance)).append("\n");
            sb.append("  Population std dev (σ):   ").append(fmt(populationStdDev)).append("\n");
            sb.append("  Sample variance (s²):     ").append(fmt(sampleVariance)).append("\n");
            sb.append("  Sample std dev (s):       ").append(fmt(sampleStdDev)).append("\n\n");

            sb.append("QUARTILES\n");
            sb.append("  Q1 (25th percentile): ").append(fmt(q1)).append("\n");
            sb.append("  Q2 (50th percentile): ").append(fmt(median)).append("\n");
            sb.append("  Q3 (75th percentile): ").append(fmt(q3)).append("\n");
            sb.append("  IQR (Q3 - Q1): ").append(fmt(iqr)).append("\n\n");

            sb.append("SUMS\n");
            sb.append("  Sum: ").append(fmt(sum)).append("\n");
            sb.append("  Sum of squares: ").append(fmt(sumOfSquares));

            return sb.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // NUMBER THEORY
    // ────────────────────────────────────────────────────────────────────────

    @Tool(name = "number_properties", description = "Analyze properties of an integer: "
            + "factors, prime factorization, divisibility, GCD/LCM with another number, "
            + "and whether it's prime, even/odd, perfect, etc.")
    public String numberProperties(
            @ToolParam(description = "The integer to analyze") long number,
            @ToolParam(description = "Optional second integer for GCD/LCM calculation", required = false) Long secondNumber) {

        StringBuilder sb = new StringBuilder();
        sb.append("Number Properties: ").append(number).append("\n");
        sb.append("──────────────────").append("─".repeat(String.valueOf(number).length())).append("\n\n");

        // Basic properties
        sb.append("CLASSIFICATION\n");
        sb.append("  Even/Odd: ").append(number % 2 == 0 ? "Even" : "Odd").append("\n");
        sb.append("  Positive/Negative: ").append(number > 0 ? "Positive" : number < 0 ? "Negative" : "Zero").append("\n");
        sb.append("  Prime: ").append(isPrime(Math.abs(number)) ? "Yes" : "No").append("\n");
        sb.append("  Perfect square: ").append(isPerfectSquare(Math.abs(number)) ? "Yes (√" + Math.abs(number) + " = " + (long) Math.sqrt(Math.abs(number)) + ")" : "No").append("\n");

        long absNum = Math.abs(number);
        if (absNum > 0 && absNum <= 1_000_000) {
            // Factors
            java.util.List<Long> factors = new java.util.ArrayList<>();
            for (long i = 1; i * i <= absNum; i++) {
                if (absNum % i == 0) {
                    factors.add(i);
                    if (i != absNum / i) factors.add(absNum / i);
                }
            }
            java.util.Collections.sort(factors);
            sb.append("  Number of factors: ").append(factors.size()).append("\n");
            sb.append("  Factors: ").append(factors).append("\n");

            // Perfect number check
            long factorSum = factors.stream().mapToLong(Long::longValue).sum() - absNum;
            sb.append("  Sum of proper divisors: ").append(factorSum);
            if (factorSum == absNum) sb.append(" (Perfect number!)");
            else if (factorSum > absNum) sb.append(" (Abundant number)");
            else sb.append(" (Deficient number)");
            sb.append("\n");

            // Prime factorization
            sb.append("\nPRIME FACTORIZATION\n");
            sb.append("  ").append(absNum).append(" = ").append(primeFactorization(absNum)).append("\n");
        }

        // GCD/LCM if second number provided
        if (secondNumber != null) {
            long gcd = gcd(Math.abs(number), Math.abs(secondNumber));
            long lcm = Math.abs(number) / gcd * Math.abs(secondNumber);
            sb.append("\nGCD & LCM with ").append(secondNumber).append("\n");
            sb.append("  GCD(").append(number).append(", ").append(secondNumber).append(") = ").append(gcd).append("\n");
            sb.append("  LCM(").append(number).append(", ").append(secondNumber).append(") = ").append(lcm);
        }

        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────
    // TRIGONOMETRY
    // ────────────────────────────────────────────────────────────────────────

    @Tool(name = "trigonometry", description = "Calculate trigonometric values and solve right triangles. "
            + "Provide an angle in degrees to get all trig values (sin, cos, tan, etc.), "
            + "or provide two sides of a right triangle to solve for all unknowns.")
    public String trigonometry(
            @ToolParam(description = "Mode: 'angle' to compute trig values for an angle, "
                    + "or 'right_triangle' to solve a right triangle") String mode,
            @ToolParam(description = "For 'angle': the angle in degrees (e.g. '45'). "
                    + "For 'right_triangle': two known values as 'type1=val1,type2=val2' where types are "
                    + "'a' (opposite), 'b' (adjacent), 'c' (hypotenuse), or 'angle' in degrees. "
                    + "Example: 'a=3,b=4' or 'c=10,angle=30'") String values) {

        try {
            return switch (mode.strip().toLowerCase()) {
                case "angle" -> {
                    double deg = Double.parseDouble(values.strip());
                    double rad = Math.toRadians(deg);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Trigonometric Values for ").append(fmt(deg)).append("°\n");
                    sb.append("─────────────────────────────\n");
                    sb.append("  Radians: ").append(fmt(rad)).append("\n\n");
                    sb.append("  sin(").append(fmt(deg)).append("°) = ").append(fmt(Math.sin(rad))).append("\n");
                    sb.append("  cos(").append(fmt(deg)).append("°) = ").append(fmt(Math.cos(rad))).append("\n");
                    sb.append("  tan(").append(fmt(deg)).append("°) = ");
                    if (Math.abs(Math.cos(rad)) < 1e-12) sb.append("undefined (division by zero)");
                    else sb.append(fmt(Math.tan(rad)));
                    sb.append("\n\n");
                    sb.append("  csc(").append(fmt(deg)).append("°) = ");
                    if (Math.abs(Math.sin(rad)) < 1e-12) sb.append("undefined");
                    else sb.append(fmt(1 / Math.sin(rad)));
                    sb.append("\n");
                    sb.append("  sec(").append(fmt(deg)).append("°) = ");
                    if (Math.abs(Math.cos(rad)) < 1e-12) sb.append("undefined");
                    else sb.append(fmt(1 / Math.cos(rad)));
                    sb.append("\n");
                    sb.append("  cot(").append(fmt(deg)).append("°) = ");
                    if (Math.abs(Math.sin(rad)) < 1e-12) sb.append("undefined");
                    else sb.append(fmt(Math.cos(rad) / Math.sin(rad)));
                    yield sb.toString();
                }

                case "right_triangle" -> solveRightTriangle(values);

                default -> "Error: Unknown mode '" + mode + "'. Use 'angle' or 'right_triangle'.";
            };
        } catch (NumberFormatException e) {
            return "Error: Invalid number format. " + e.getMessage();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────────────

    private String solveRightTriangle(String input) {
        // Parse key=value pairs
        Double a = null, b = null, c = null, angle = null;

        for (String part : input.split(",")) {
            String[] kv = part.strip().split("=");
            if (kv.length != 2) continue;
            double val = Double.parseDouble(kv[1].strip());
            switch (kv[0].strip().toLowerCase()) {
                case "a" -> a = val;
                case "b" -> b = val;
                case "c" -> c = val;
                case "angle" -> angle = val;
            }
        }

        // Solve for unknowns
        if (a != null && b != null) {
            c = Math.sqrt(a * a + b * b);
            angle = Math.toDegrees(Math.atan(a / b));
        } else if (a != null && c != null) {
            b = Math.sqrt(c * c - a * a);
            angle = Math.toDegrees(Math.asin(a / c));
        } else if (b != null && c != null) {
            a = Math.sqrt(c * c - b * b);
            angle = Math.toDegrees(Math.acos(b / c));
        } else if (c != null && angle != null) {
            double rad = Math.toRadians(angle);
            a = c * Math.sin(rad);
            b = c * Math.cos(rad);
        } else if (a != null && angle != null) {
            double rad = Math.toRadians(angle);
            b = a / Math.tan(rad);
            c = a / Math.sin(rad);
        } else if (b != null && angle != null) {
            double rad = Math.toRadians(angle);
            a = b * Math.tan(rad);
            c = b / Math.cos(rad);
        } else {
            return "Error: Need at least two known values (a, b, c, angle).";
        }

        double otherAngle = 90 - angle;
        double area = 0.5 * a * b;
        double perimeter = a + b + c;

        StringBuilder sb = new StringBuilder();
        sb.append("Right Triangle Solution\n");
        sb.append("───────────────────────\n\n");
        sb.append("SIDES\n");
        sb.append("  a (opposite):   ").append(fmt(a)).append("\n");
        sb.append("  b (adjacent):   ").append(fmt(b)).append("\n");
        sb.append("  c (hypotenuse): ").append(fmt(c)).append("\n\n");
        sb.append("ANGLES\n");
        sb.append("  θ = ").append(fmt(angle)).append("°\n");
        sb.append("  φ = ").append(fmt(otherAngle)).append("°\n");
        sb.append("  Right angle = 90°\n\n");
        sb.append("MEASUREMENTS\n");
        sb.append("  Area = ½ab = ").append(fmt(area)).append("\n");
        sb.append("  Perimeter = ").append(fmt(perimeter)).append("\n\n");
        sb.append("VERIFICATION\n");
        sb.append("  a² + b² = ").append(fmt(a * a + b * b)).append("\n");
        sb.append("  c²       = ").append(fmt(c * c)).append(" ✓");

        return sb.toString();
    }

    private String classifyTriangle(double a, double b, double c, double angleA, double angleB, double angleC) {
        java.util.List<String> types = new java.util.ArrayList<>();

        if (Math.abs(a - b) < 1e-9 && Math.abs(b - c) < 1e-9) types.add("Equilateral");
        else if (Math.abs(a - b) < 1e-9 || Math.abs(b - c) < 1e-9 || Math.abs(a - c) < 1e-9) types.add("Isosceles");
        else types.add("Scalene");

        double maxAngle = Math.max(angleA, Math.max(angleB, angleC));
        if (Math.abs(maxAngle - 90) < 0.01) types.add("Right");
        else if (maxAngle > 90) types.add("Obtuse");
        else types.add("Acute");

        return String.join(", ", types);
    }

    private boolean isPrime(long n) {
        if (n < 2) return false;
        if (n < 4) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (long i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private boolean isPerfectSquare(long n) {
        long sqrt = (long) Math.sqrt(n);
        return sqrt * sqrt == n;
    }

    private String primeFactorization(long n) {
        if (n <= 1) return String.valueOf(n);
        java.util.List<String> factors = new java.util.ArrayList<>();
        for (long p = 2; p * p <= n; p++) {
            int count = 0;
            while (n % p == 0) { count++; n /= p; }
            if (count > 0) {
                factors.add(count > 1 ? p + "^" + count : String.valueOf(p));
            }
        }
        if (n > 1) factors.add(String.valueOf(n));
        return String.join(" × ", factors);
    }

    private long gcd(long a, long b) {
        while (b != 0) { long t = b; b = a % b; a = t; }
        return a;
    }

    private String computeMode(double[] sorted) {
        java.util.Map<Double, Integer> freq = new java.util.LinkedHashMap<>();
        for (double v : sorted) freq.merge(v, 1, Integer::sum);
        int maxFreq = freq.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maxFreq == 1) return "No mode (all values unique)";
        java.util.List<String> modes = freq.entrySet().stream()
                .filter(e -> e.getValue() == maxFreq)
                .map(e -> fmt(e.getKey()))
                .toList();
        return String.join(", ", modes) + " (frequency: " + maxFreq + ")";
    }

    private double percentile(double[] sorted, double p) {
        double index = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted[lower];
        return sorted[lower] + (index - lower) * (sorted[upper] - sorted[lower]);
    }

    private double[] parseValues(String values) {
        String[] parts = values.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].strip());
        }
        return result;
    }

    private void checkArgs(double[] args, int required, String names) {
        if (args.length < required) {
            throw new IllegalArgumentException("Need " + required + " values: " + names);
        }
    }

    private String fmt(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "∞" : "-∞";
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value)
                .round(MC)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String formatQuadratic(double a, double b, double c) {
        StringBuilder sb = new StringBuilder();
        if (a == 1) sb.append("x²");
        else if (a == -1) sb.append("-x²");
        else sb.append(fmt(a)).append("x²");

        if (b > 0) sb.append(" + ").append(b == 1 ? "" : fmt(b)).append("x");
        else if (b < 0) sb.append(" - ").append(b == -1 ? "" : fmt(-b)).append("x");

        if (c > 0) sb.append(" + ").append(fmt(c));
        else if (c < 0) sb.append(" - ").append(fmt(-c));

        return sb.toString();
    }

    private String formatLinear(double a, double b, double c) {
        return fmt(a) + "x + " + fmt(b) + "y = " + fmt(c);
    }

    private String formatPolynomial(double[] coeffs) {
        int degree = coeffs.length - 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= degree; i++) {
            double c = coeffs[i];
            int power = degree - i;
            if (c == 0 && degree > 0) continue;
            if (sb.length() > 0 && c > 0) sb.append(" + ");
            else if (sb.length() > 0 && c < 0) sb.append(" - ");
            else if (c < 0) sb.append("-");

            double absC = Math.abs(c);
            if (power == 0 || absC != 1) sb.append(fmt(absC));
            if (power > 1) sb.append("x^").append(power);
            else if (power == 1) sb.append("x");
        }
        return sb.toString();
    }

    private String format(String title, String input, String result) {
        return title + "\n" + "─".repeat(title.length()) + "\n" + input + "\n\n" + result;
    }

    private String formatShape(String name, String dims, String line1, String line2, String line3) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("─".repeat(name.length())).append("\n");
        sb.append("Dimensions: ").append(dims).append("\n\n");
        sb.append(line1).append("\n");
        if (line2 != null) sb.append(line2).append("\n");
        if (line3 != null) sb.append(line3).append("\n");
        return sb.toString();
    }

    private String formatArray(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            if (i > 20) { sb.append("... ").append(arr.length - 21).append(" more"); break; }
            sb.append(fmt(arr[i]));
        }
        return sb.append("]").toString();
    }
}
