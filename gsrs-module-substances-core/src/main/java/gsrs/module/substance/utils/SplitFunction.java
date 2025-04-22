package gsrs.module.substance.utils;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SplitFunction extends BaseFunction {
    public SplitFunction() {
        super(ArgumentConstraints.listOf(2, 3, ArgumentConstraints.anyValue()));
    }

    @Override
    protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
        T subject = arguments.get(0).value();
        T delimiter = arguments.get(1).value();
        int limit = -1;
        if (arguments.size() > 2) {
            T value = arguments.get(2).value();
            if (runtime.typeOf(value) == JmesPathType.NUMBER) {
                limit = (int)runtime.toNumber(value).intValue();
            }
        }
        List<String> lst = Arrays.asList(runtime.toString(subject).split(runtime.toString(delimiter), limit));
        return runtime.createArray(lst.stream().map(p->runtime.createString(p)).collect(Collectors.toList()));
    }
}
