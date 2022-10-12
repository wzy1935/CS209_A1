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
        var tmp = ma.getCoStarCount();
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
        System.out.println(t);
        return t;
    }

    public List<String> getTopMovies(int top_k, String by) {
        return null;
    }

    public List<String> getTopStars(int top_k, String by) {
        return null;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return null;
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
    }

    static class Utils {
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

                    // work with one line
                    boolean inQuote = false;
                    boolean tran = false;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (tran) {
                            tran = false;
                            sb.append(c);
                            continue;
                        }
                        if (c == '\\') {
                            tran = true;
                            continue;
                        }
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
                            inQuote = !inQuote;
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

