package hr.algebra.photoapp.util;

import hr.algebra.photoapp.model.Photo;
import hr.algebra.photoapp.model.UserAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FunctionalPhotoHelpersTest {

    @Test
    void normalizeHashtags_formatsAndDeduplicates() {
        String result = FunctionalPhotoHelpers.normalizeHashtags(" nature, travel #nature ");

        assertEquals("#nature #travel", result);
    }

    @Test
    void totalPhotoBytes_sumsSizes() {
        Photo a = Photo.builder().size(100).build();
        Photo b = Photo.builder().size(250).build();

        assertEquals(350, FunctionalPhotoHelpers.totalPhotoBytes(List.of(a, b)));
    }

    @Test
    void countActionsByType_groupsByActionName() {
        UserAction upload = UserAction.builder().action("UPLOAD_PHOTO").build();
        UserAction login = UserAction.builder().action("LOGIN").build();
        UserAction upload2 = UserAction.builder().action("UPLOAD_PHOTO").build();

        Map<String, Long> counts = FunctionalPhotoHelpers.countActionsByType(
                List.of(upload, login, upload2));

        assertEquals(2L, counts.get("UPLOAD_PHOTO"));
        assertEquals(1L, counts.get("LOGIN"));
    }

    @Test
    void canModifyPhoto_allowsOwnerOrAdmin() {
        Photo photo = Photo.builder()
                .owner(hr.algebra.photoapp.model.User.builder().username("alice").build())
                .build();

        assertTrue(FunctionalPhotoHelpers.canModifyPhoto(
                photo, "alice", role -> false));
        assertTrue(FunctionalPhotoHelpers.canModifyPhoto(
                photo, "bob", role -> "ROLE_ADMIN".equals(role)));
        assertFalse(FunctionalPhotoHelpers.canModifyPhoto(
                photo, "bob", role -> false));
    }

    @Test
    void filterByHashtag_returnsMatchingPhotos() {
        Photo match = Photo.builder().hashtags("#nature #sun").build();
        Photo other = Photo.builder().hashtags("#city").build();

        List<Photo> filtered = FunctionalPhotoHelpers.filterByHashtag(
                List.of(match, other), "nature");

        assertEquals(1, filtered.size());
        assertSame(match, filtered.get(0));
    }
}
