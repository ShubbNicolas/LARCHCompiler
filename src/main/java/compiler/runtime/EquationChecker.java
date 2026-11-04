package compiler.runtime;

import compiler.parser.Ast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 等式检查器：对 {@code asserts} 段里的每条等式，枚举变量在其排序取值域上的组合，
 * 代入左右两边求值并比较。
 *
 * <p>这是 LSL 这种声明式规格最贴近的「运行」方式 —— 没有可执行语句，
 * 只能验证等式在有限样本上是否成立。</p>
 */
public final class EquationChecker {

    private EquationChecker() {
    }

    public static List<CheckResult> check(Ast.Trait trait) {
        List<CheckResult> results = new ArrayList<>();
        if (trait == null) {
            return results;
        }
        Ast.Asserts asserts = trait.sections().stream()
                .filter(Ast.Asserts.class::isInstance)
                .map(Ast.Asserts.class::cast)
                .findFirst()
                .orElse(null);
        if (asserts == null) {
            return results;
        }

        List<String> names = new ArrayList<>();
        List<List<Value>> domains = new ArrayList<>();
        for (Ast.VarDecl decl : asserts.variables()) {
            List<Value> domain = Domains.of(decl.sort());
            for (String name : decl.names()) {
                names.add(name);
                domains.add(domain);
            }
        }

        int index = 0;
        for (Ast.Equation equation : asserts.equations()) {
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
                        "左 = " + Value.show(left) + "，右 = " + Value.show(right));
            }
        }

        if (firstUnknown != null) {
            return new CheckResult(index, CheckResult.Status.UNKNOWN, unknownWitness, firstUnknown);
        }
        return new CheckResult(index, CheckResult.Status.PASS, Map.of(), "");
    }

    private static Map<String, String> witness(List<String> names, Map<String, Value> env) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : names) {
            map.put(name, Value.show(env.get(name)));
        }
        return map;
    }

    /** 汇总为可读报告，供控制台输出。 */
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
}
