import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public record Result<V, E extends Throwable>(V value, E Throwable, boolean success) {
    public static <V, E extends Throwable> Result<V, E> isSuccess(V value) {
        return new Result<>(value, null, true);
    }

    public static <V, E extends Throwable> Result<V, E> failure(E throwable) {
        return new Result<>(null, throwable, false);
    }

    static Result<String, IOException> safeReadString(Path path) {
        try{
            return Result.isSuccess(Files.readString(path));
        } catch (IOException ex){
            return Result.failure(ex);
        }
    }
}

class Derived{
    public static void main(String[] args) {
        long count = Stream.of(Paths.get("war-and-peace.txt"))
                .map(Result::safeReadString)
                .filter(Result::success)
                .count();

        System.out.println(count); // => 1

    }
}
