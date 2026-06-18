package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Assert Utility Class Tests")
class AssertTest {
    @Test
    @DisplayName("Should throw exception when trying to instantiate utility class")
    void shouldCoverPrivateConstructor() throws NoSuchMethodException {
        Constructor<Assert> constructor = Assert.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Assert class cannot be instantiated");
    }

    @Nested
    @DisplayName("Tests for hasText method")
    class HasTextTests {
        @Test
        @DisplayName("Should throw exception when text is null")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> Assert.hasText(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Text cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw exception when text is blank or empty")
        void shouldThrowExceptionWhenBlank(String blankString) {
            assertThatThrownBy(() -> Assert.hasText(blankString))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Text cannot be blank");
        }

        @Test
        @DisplayName("Should not throw exception when text is valid")
        void shouldNotThrowExceptionWhenValid() {
            assertThatNoException().isThrownBy(() -> Assert.hasText("Valid text"));
        }
    }

    @Nested
    @DisplayName("Tests for notNull method")
    class NotNullTests {
        @Test
        @DisplayName("Should throw exception when object is null")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> Assert.notNull(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object cannot be null");
        }

        @Test
        @DisplayName("Should not throw exception when object is present")
        void shouldNotThrowExceptionWhenNotNull() {
            assertThatNoException().isThrownBy(() -> Assert.notNull(new Object()));
        }
    }

    @Nested
    @DisplayName("Tests for notEmpty method")
    class NotEmptyTests {
        @Test
        @DisplayName("Should throw exception when list is null (delegates to notNull)")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> Assert.notEmpty(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when list is empty")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> Assert.notEmpty(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("List cannot be empty");
        }

        @Test
        @DisplayName("Should not throw exception when list has elements")
        void shouldNotThrowExceptionWhenValid() {
            assertThatNoException().isThrownBy(() -> Assert.notEmpty(List.of("Element")));
        }
    }

    @Nested
    @DisplayName("Tests for isInstanceOf method")
    class IsInstanceOfTests {
        @Test
        @DisplayName("Should throw exception when object is null")
        void shouldThrowExceptionWhenObjectIsNull() {
            assertThatThrownBy(() -> Assert.isInstanceOf(null, String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when class is null")
        void shouldThrowExceptionWhenClassIsNull() {
            assertThatThrownBy(() -> Assert.isInstanceOf("Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object cannot be null");
        }

        @Test
        @DisplayName("Should return true when object is instance of class")
        void shouldReturnTrueWhenIsInstance() {
            assertThat(Assert.isInstanceOf("Test string", String.class)).isTrue();
        }

        @Test
        @DisplayName("Should return false when object is NOT instance of class")
        void shouldReturnFalseWhenIsNotInstance() {
            assertThat(Assert.isInstanceOf(123, String.class)).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for hasLength method")
    class HasLengthTests {
        @Test
        @DisplayName("Should throw exception when text is null (delegates to notNull)")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> Assert.hasLength(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Object cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when text is empty")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> Assert.hasLength(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Text cannot be empty");
        }

        @Test
        @DisplayName("Should not throw exception when text has length (even whitespace)")
        void shouldNotThrowExceptionWhenHasLength() {
            assertThatNoException().isThrownBy(() -> Assert.hasLength(" "));
            assertThatNoException().isThrownBy(() -> Assert.hasLength("Text"));
        }
    }
}
