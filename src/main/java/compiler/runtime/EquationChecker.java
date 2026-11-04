package compiler.runtime;

import compiler.parser.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规格一致性检查。
 *
 * <p>按 LSL 的语义分层：</p>
 * <ul>
 *   <li>{@code introduces} —— 算子签名，只列出不发生验证</li>
 *   <li>{@code asserts} —— 算子的等式定义，检查其自洽性（每条等式在各取值下成立）</li>
 *   <li>{@code implies} —— 由 asserts 推得、需要被包含者满足的性质，检查其是否成立</li>
 * </ul>
 *
 * <p>检查方式是有限枚举：在变量排序的取值域上逐组代入，未定义（⊥）的取值按 LSL 惯例跳过。</p>
 */
public final class EquationChecker {

    private EquationChecker() {
    }

    /** 只检查 asserts 段（兼容旧接口）。 */
    public static List<CheckResult> check(Ast.Trait trait) {
        if (trait == null) {
            return List.of();
        }
        Ast.Asserts asserts = find(trait, Ast.Asserts.class);
        return asserts == null ? List.of() : checkEquations(asserts.equations(), asserts.variables());
    }

    /**
     * 完整检查一个 trait：asserts 定义是否自洽、implies 推论是否成立。
     */
    public static SpecReport checkSpec(Ast.Trait trait) {
        if (trait == null) {
            return new SpecReport("(无)", List.of(), List.of(), List.of(), List.of(),
                    List.of(), SpecReport.Verdict.UNVERIFIABLE);
        }

        Ast.Introduces intro = find(trait, Ast.Introduces.class);
        List<String> operators = intro == null
                ? List.of()
                : intro.signatures().stream()
                        .map(s -> s.name() + "/" + s.argumentSorts().size())
                        .toList();

        Ast.Asserts asserts = find(trait, Ast.Asserts.class);
        List<CheckResult> assertResults = asserts == null
                ? List.of()
                : checkEquations(asserts.equations(), asserts.variables());

        Ast.Implies implies = find(trait, Ast.Implies.class);
        List<CheckResult> impliesResults = implies == null
                ? List.of()
                : checkEquations(implies.equations(), implies.variables());

        Ast.Converts converts = find(trait, Ast.Converts.class);
        List<String> convertNames = converts == null
                ? List.of()
                : converts.operators().stream().map(Ast.Ref::name).toList();

        List<StructureCheck.Finding> findings = StructureCheck.check(trait);

        return new SpecReport(trait.name(), operators, assertResults, impliesResults,
                convertNames, findings, verdictOf(assertResults, impliesResults));
    }

    private static SpecReport.Verdict verdictOf(List<CheckResult> asserts, List<CheckResult> implies) {
        boolean anyFail = asserts.stream().anyMatch(r -> r.status() == CheckResult.Status.FAIL)
                || implies.stream().anyMatch(r -> r.status() == CheckResult.Status.FAIL);
        if (anyFail) {
            return SpecReport.Verdict.CONTRADICTION;
        }
        boolean anyUnknown = asserts.stream().anyMatch(r -> r.status() == CheckResult.Status.UNKNOWN)
                || implies.stream().anyMatch(r -> r.status() == CheckResult.Status.UNKNOWN);
        return anyUnknown ? SpecReport.Verdict.UNVERIFIABLE : SpecReport.Verdict.CONSISTENT;
    }

    @SuppressWarnings("unchecked")
    private static <T> T find(Ast.Trait trait, Class<T> type) {
        return (T) trait.sections().stream()
                .filter(type::isInstance)
                .findFirst()
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // 枚举检查
    // ------------------------------------------------------------------

    private static List<CheckResult> checkEquations(List<Ast.Equation> equations,
                                                    List<Ast.VarDecl> varDecls) {
        List<String> names = new ArrayList<>();
        List<List<Value>> domains = new ArrayList<>();
        for (Ast.VarDecl decl : varDecls) {
            List<Value> domain = Domains.of(decl.sort());
            for (String name : decl.names()) {
                names.add(name);
                domains.add(domain);
            }
        }

        List<CheckResult> results = new ArrayList<>();
        int index = 0;
        for (Ast.Equation equation : equations) {
            index++;
            results.add(checkOne(index, equation, names, domains));
        }
        return results;
    }

    private static CheckResult checkOne(int index, Ast.Equation equation,
                                        List<String> names, List<List<Value>> domains) {
        Map<String, Value> env = new LinkedHashMap<>();
        int combinations = 1;
        for (List<Value> domain : domains) {
            combinations *= Math.max(domain.size(), 1);
        }

        int skipped = 0;
        String firstUnknown = null;
        Map<String, String> unknownWitness = Map.of();

        for (int n = 0; n < combinations; n++) {
            int rest = n;
            for (int i = 0; i < names.size(); i++) {
                List<Value> domain = domains.get(i);
                env.put(names.get(i), domain.get(rest % domain.size()));
                rest /= domain.size();
            }

            Value left = Evaluator.eval(equation.left(), env);
            Value right = Evaluator.eval(equation.right(), env);

            // 术语未定义：LSL 不要求该取值下等式成立，跳过
            if (left instanceof Value.Undefined || right instanceof Value.Undefined) {
                skipped++;
                continue;
            }
            if (left instanceof Value.Unknown unknown) {
                if (firstUnknown == null) {
                    firstUnknown = "左侧: " + unknown.reason();
                    unknownWitness = witness(names, env);
                }
                continue;
            }
            if (right instanceof Value.Unknown unknown) {
                if (firstUnknown == null) {
                    firstUnknown = "右侧: " + unknown.reason();
                    unknownWitness = witness(names, env);
                }
                continue;
            }

            if (!Evaluator.same(left, right)) {
                return new CheckResult(index, CheckResult.Status.FAIL, witness(names, env),
                        "左 = " + Value.show(left) + "，右 = " + Value.show(right), skipped);
            }
        }

        if (firstUnknown != null) {
            return new CheckResult(index, CheckResult.Status.UNKNOWN, unknownWitness,
                    firstUnknown, skipped);
        }
        return new CheckResult(index, CheckResult.Status.PASS, Map.of(), "", skipped);
    }

    private static Map<String, String> witness(List<String> names, Map<String, Value> env) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : names) {
            map.put(name, Value.show(env.get(name)));
        }
        return map;
    }

    /** 汇总为可读报告（只含 asserts）。 */
    public static String report(List<CheckResult> results) {
        if (results.isEmpty()) {
            return "没有可检查的等式。\n";
        }
        long pass = results.stream().filter(r -> r.status() == CheckResult.Status.PASS).count();
        long fail = results.stream().filter(r -> r.status() == CheckResult.Status.FAIL).count();
        long unknown = results.stream().filter(r -> r.status() == CheckResult.Status.UNKNOWN).count();

        StringBuilder sb = new StringBuilder();
        for (CheckResult r : results) {
            sb.append(r.summarize()).append('\n');
        }
        sb.append(String.format("%n共 %d 条等式：通过 %d，失败 %d，无法求值 %d%n",
                results.size(), pass, fail, unknown));
        return sb.toString();
    }

    /** 规格检查报告。 */
    public record SpecReport(String traitName, List<String> operators,
                             List<CheckResult> asserts, List<CheckResult> implies,
                             List<String> converts, List<StructureCheck.Finding> findings,
                             Verdict verdict) {

        public enum Verdict {
            /** asserts 与 implies 在枚举范围内均成立 */
            CONSISTENT,
            /** 存在反例，规格自相矛盾 */
            CONTRADICTION,
            /** 有无法求值的部分，不能断言一致 */
            UNVERIFIABLE
        }

        public SpecReport {
            operators = List.copyOf(operators);
            asserts = List.copyOf(asserts);
            implies = List.copyOf(implies);
            converts = List.copyOf(converts);
            findings = List.copyOf(findings);
        }

        public String render() {
            StringBuilder sb = new StringBuilder();
            sb.append("Trait ").append(traitName).append('\n');

            sb.append("  introduces: ");
            sb.append(operators.isEmpty() ? "(无)" : String.join(", ", operators));
            sb.append('\n');

            if (!asserts.isEmpty()) {
                sb.append("  asserts（定义自洽性）: ").append(asserts.size()).append(" 条\n");
                for (CheckResult r : asserts) {
                    sb.append("    ").append(r.summarize()).append('\n');
                }
            }

            if (!implies.isEmpty()) {
                sb.append("  implies（推论）: ").append(implies.size()).append(" 条\n");
                for (CheckResult r : implies) {
                    sb.append("    ").append(r.summarize()).append('\n');
                }
            }

            if (!converts.isEmpty()) {
                sb.append("  converts: ").append(String.join(", ", converts)).append('\n');
            }

            sb.append("  结构检查（introduces ↔ asserts）: ");
            if (findings.isEmpty()) {
                sb.append("声明与定义一致\n");
            } else {
                sb.append(findings.size()).append(" 条警告\n");
                for (StructureCheck.Finding finding : findings) {
                    sb.append("    - ").append(finding).append('\n');
                }
            }

            sb.append("  结论: ").append(verdictText()).append('\n');
            return sb.toString();
        }

        private String verdictText() {
            return switch (verdict) {
                case CONSISTENT -> "规格自洽（asserts 成立，implies 推论亦成立；基于有限枚举）";
                case CONTRADICTION -> "规格矛盾（存在反例，见上）";
                case UNVERIFIABLE -> "部分无法验证（存在未实现算子或未知排序）";
            };
        }
    }
}
