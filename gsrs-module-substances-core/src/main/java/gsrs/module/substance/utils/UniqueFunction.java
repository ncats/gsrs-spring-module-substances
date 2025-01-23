package gsrs.module.substance.utils;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;

import java.util.List;
import java.util.stream.Collectors;

public class UniqueFunction extends BaseFunction {
    public UniqueFunction() {
        super(ArgumentConstraints.typeOf(JmesPathType.ARRAY));
    }

    @Override
    protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
        T array = arguments.get(0).value();
        return runtime.createArray(runtime.toList(array).stream().distinct().collect(Collectors.toList()));
    }
}
