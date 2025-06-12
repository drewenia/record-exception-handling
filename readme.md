Optional'in getirdiği ağır yükten ve exception handling'in Stream'in pipeline'i içerisinde yazılan try/catch
block'larının, Stream'in genel amaçlarına ters düşmesinden dolayı, best practice olarak Record kullanılabilir;

Result type'ının temel amacı, olası bir değeri veya başarılı olunamazsa başarısızlığın nedenini represent eden bir
`Exception`'ı tutmaktır.

```
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
```

Record component'leri farklı state'leri reflect eder. Explicit `success` field'i, başarılı bir operation'ı daha iyi
belirlemeye ve `null`'u geçerli bir value olarak desteklemeye yardımcı olur. Kolaylık sağlayan factory metotları daha
anlamlı bir API sunar. Bu basit iskele `(scaffold)` bile, uygun result'lar oluşturmak için anlamlı bir yol olan
convenience factory metotlarıyla, `Optional` kullanmaya göre belirli bir iyileştirme sunar.

Basitçe bir main method içerisinde kullanılabilir;

```
public static void main(String[] args) {
    long result = Stream.of(Paths.get("war-and-peace.txt"),
                    Paths.get("invalid"),
                    Paths.get("example-10-6/result-as-return-type.jsh"))
            .map(Result::safeReadString)
            .filter(Result::success)
            .count();
    
    System.out.println(result); // 1
}
```