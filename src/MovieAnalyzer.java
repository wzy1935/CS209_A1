import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {
    List<Movie> movies = new ArrayList<>();

    //test
    public static void main(String[] args) {
        MovieAnalyzer ma = new MovieAnalyzer("resources/imdb_top_500.csv");
        var tmp = ma.getTopStars(50, "gross");
        System.out.println(tmp);
    }

    public MovieAnalyzer(String datasetPath) {
        List<List<String>> data = Utils.readCsv(datasetPath);
        boolean firstLine =  true;
        for (List<String> line : data) {
            while (line.size() < 16) {
                line.add(null);
            }
            if (firstLine) {
                firstLine = false;
                continue;
            }
            Movie movie = new Movie();
            movie.seriesTitle = line.get(1);
            movie.releasedYear = Utils.parseInt(line.get(2));
            movie.certificate = line.get(3);
            movie.runtime = Utils.parseInt(line.get(4).split(" ")[0]);
            movie.genre = List.of(line.get(5).split(",\\s?"));
            movie.IMDBRating = Utils.parseDouble(line.get(6));
            movie.overview = line.get(7);
            movie.metaScore = Utils.parseInt(line.get(8));
            movie.director = line.get(9);
            movie.stars = new ArrayList<>();
            for (int i = 10; i <= 13; i++) {
                if (!"".equals(line.get(i)) && line.get(i) != null) movie.stars.add(line.get(i));
            }
            movie.numOfVotes = Utils.parseLong(line.get(14));
            movie.gross = line.get(15) == null ? null : Utils.parseLong(line.get(15).replace("," , ""));
            movies.add(movie);
        }
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        return movies.stream().collect(Collectors.groupingBy(
                movie -> movie.releasedYear,
                Collectors.summingInt(e -> 1)
        )).entrySet().stream().sorted(Map.Entry.comparingByKey(
                (t0, t1) -> t1 - t0
        )).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldVal, newVal) -> oldVal,
                LinkedHashMap::new));
    }

    public Map<String, Integer> getMovieCountByGenre() {
        return movies.stream().map(
                movie -> movie.genre
        ).flatMap(
                Collection::stream
        ).collect(Collectors.toList()).stream().collect(Collectors.groupingBy(
                i -> i,
                Collectors.summingInt(e -> 1))
        ).entrySet().stream().sorted(
                Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())
        ).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldVal, newVal) -> oldVal,
                LinkedHashMap::new));
    }

    public Map<List<String>, Integer> getCoStarCount() {
        var t = movies.stream().filter(movie -> movie.stars.size() >= 2).map(movie -> {
            List<List<String>> result = new ArrayList<>();
            List<String> names = movie.stars;
            for (int i = 0; i < names.size(); i++) {
                for (int j = i+1; j < names.size(); j++) {
                    result.add(Stream.of(names.get(i), names.get(j)).sorted().collect(Collectors.toList()));
                }
            }
            return result;
        })
                .flatMap(Collection::stream).collect(Collectors.toList()).stream()
                .collect(Collectors.groupingBy(
                        i -> i,
                        Collectors.summingInt(e -> 1)
                ));
        return t;
    }

    public List<String> getTopMovies(int topK, String by) {
        var t = movies.stream().sorted(
            by.equals("runtime") ?
                    Comparator.comparing((Function<Movie, Integer>) movie -> movie.runtime).reversed().
                            thenComparing(movie -> movie.seriesTitle)
                    :
                    Comparator.comparing((Function<Movie, Integer>) movie -> movie.overview.length()).reversed().
                            thenComparing(movie -> movie.seriesTitle)
        )
                .limit(topK).map(m -> m.seriesTitle).collect(Collectors.toList());
        return t;
    }

    public List<String> getTopStars(int topK, String by) {
        var t = movies.stream()
                .filter(m -> by.equals("rating") ? m.IMDBRating != null : m.gross != null)
                .flatMap(movie -> movie.stars.stream().map(s -> new Utils.Pair<>(s, movie)))
                .collect(Collectors.groupingBy(p -> p.first)).entrySet().stream().sorted(
                        Map.Entry.<String, List<Utils.Pair<String, Movie>>>comparingByValue(
                                Comparator.< List<Utils.Pair<String, Movie>>, Double>comparing(l -> l.stream().mapToDouble(p -> by.equals("rating") ? p.second.IMDBRating : p.second.gross).average().orElse(0)).reversed()
                        ).thenComparing(Map.Entry.comparingByKey())
                ).map(Map.Entry::getKey).limit(topK).collect(Collectors.toList());
        return t;
    }

    public List<String> searchMovies(String genre, float minRating, int maxRuntime) {
        var t = movies.stream()
                .filter(m -> m.genre.contains(genre) && m.IMDBRating >= minRating && m.runtime <= maxRuntime)
                .map(m -> m.seriesTitle).sorted().collect(Collectors.toList());
        return t;
    }

    static class Movie {
        public String seriesTitle;
        public Integer releasedYear;
        public String certificate;
        public Integer runtime;
        public List<String> genre;
        public Double IMDBRating;
        public String overview;
        public Integer metaScore;
        public String director;
        public List<String> stars;
        public Long numOfVotes;
        public Long gross;

        @Override
        public String toString() {
            return seriesTitle + " " + overview.length();
        }
    }

    static class Utils {

        static class Pair<S, T> {
            public S first;
            public T second;

            public Pair(S first, T second) {
                this.first = first;
                this.second = second;
            }
        }

        public static Integer parseInt(String s) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return null;
            }
        }

        public static Long parseLong(String s) {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                return null;
            }
        }

        public static Double parseDouble(String s) {
            try {
                return (double) Float.parseFloat(s);
            } catch (Exception e) {
                return null;
            }
        }

        public static List<List<String>> readCsv(String datasetPath) {
            List<List<String>> output = new ArrayList<>();
            try {
                Scanner s = new Scanner(new File(datasetPath), StandardCharsets.UTF_8);
                while (s.hasNextLine()) {
                    List<String> thisLine = new ArrayList<>();
                    String line = s.nextLine();
                    if (line.contains("Indiana Jones and the Last Crusade")) {
                        System.out.println();
                    }

                    // work with one line
                    boolean inQuote = false;
                    boolean doubleQuote = false;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == ',') {
                            if (inQuote) {
                                sb.append(c);
                            } else {
                                thisLine.add(sb.toString());
                                sb = new StringBuilder();
                            }
                            continue;
                        }
                        if (c == '"') {
                            if (inQuote) {
                                if (doubleQuote) { // second quote
                                    doubleQuote = false;
                                    sb.append("\"\"");
                                } else if (i != line.length()-1 && line.charAt(i+1) == '"') { // first quote
                                    doubleQuote = true;
                                } else {
                                    inQuote = false;
                                }
                            } else {
                                inQuote = true;
                            }
                            continue;
                        }
                        sb.append(c);
                    }
                    if (sb.length() != 0) {
                        thisLine.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    output.add(thisLine);
                }
                s.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return output;
        }

    }

}

