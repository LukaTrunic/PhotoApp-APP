package hr.algebra.photoapp.util;

import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.UserAction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Pure helper methods using functional programming (streams, lambdas, immutability).
 * Used across PhotoApp to reduce imperative loops and coupling.
 */
public final class FunctionalPhotoHelpers {

    private FunctionalPhotoHelpers() {
    }

    /**
     * Example 1 — map, filter, distinct, collect
     * Normalizes hashtag input into a consistent "#tag #tag2" string.
     */
    public static String normalizeHashtags(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        return Arrays.stream(raw.split("[\\s,]+"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .distinct()
                .collect(Collectors.joining(" "));
    }

    /**
     * Example 2 — mapToLong and reduce (sum)
     * Sums photo file sizes without a manual loop.
     */
    public static long totalPhotoBytes(List<Photo> photos) {
        return photos.stream()
                .mapToLong(Photo::getSize)
                .sum();
    }

    /**
     * Example 3 — groupingBy collector
     * Groups user actions by type for statistics.
     */
    public static Map<String, Long> countActionsByType(List<UserAction> actions) {
        return actions.stream()
                .collect(Collectors.groupingBy(UserAction::getAction, Collectors.counting()));
    }

    /**
     * Example 4 — predicate parameter (higher-order function)
     * Checks whether the current user may edit/delete a photo.
     */
    public static boolean canModifyPhoto(Photo photo, String username, Predicate<String> hasRole) {
        boolean isOwner = photo.getOwner().getUsername().equals(username);
        return isOwner || hasRole.test("ROLE_ADMIN");
    }

    /**
     * Example 5 — filter and immutable result list
     * Filters an in-memory photo list by hashtag (declarative search helper).
     */
    public static List<Photo> filterByHashtag(List<Photo> photos, String hashtag) {
        if (hashtag == null || hashtag.isBlank()) {
            return photos;
        }

        String needle = hashtag.toLowerCase();

        return photos.stream()
                .filter(photo -> photo.getHashtags() != null
                        && photo.getHashtags().toLowerCase().contains(needle))
                .toList();
    }
}
