import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public record Result<V, E extends Throwable>(V value, E throwable, boolean isSuccess) {
    public static <V, E extends Throwable> Result<V, E> success(V value) {
        return new Result<>(value, null, true);
    }

    public static <V, E extends Throwable> Result<V, E> failure(E throwable) {
        return new Result<>(null, throwable, false);
    }

    <R> Optional<R> mapSuccess(Function<V, R> fn) {
        return this.isSuccess ? Optional.ofNullable(this.value).map(fn)
                : Optional.empty();
    }

    <R> Optional<R> mapFailure(Function<E, R> fn) {
        return this.isSuccess ? Optional.empty()
                : Optional.ofNullable(this.throwable).map(fn);
    }

    <R> R map(Function<V, R> successFn, Function<E, R> failureFn) {
        return this.isSuccess ? successFn.apply(this.value)
                : failureFn.apply(this.throwable);
    }

    public void ifSuccess(Consumer<? super V> action) {
        if (this.isSuccess)
            action.accept(this.value);

    }

    public void ifFailure(Consumer<? super E> action) {
        if (!this.isSuccess)
            action.accept(this.throwable);
    }

    public void handle(Consumer<? super V> successAction, Consumer<? super E> failureAction) {
        if (this.isSuccess)
            successAction.accept(this.value);
        else
            failureAction.accept(this.throwable);
    }

    static Result<String, IOException> safeReadString(Path path) {
        try {
            return Result.success(Files.readString(path));
        } catch (IOException ex) {
            return Result.failure(ex);
        }
    }
}

class Derived {
    public static void main(String[] args) {
        Result.safeReadString(Paths.get("war-and-peace.txt"))
                .handle(
                        System.out::println, // -> success case'inde
                        failure -> System.out.println("IO-Error : " + failure.getMessage())
                );
    }
}
