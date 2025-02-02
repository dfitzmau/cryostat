/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.cryostat.rules;

import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleTest {

    static final String NAME = "fooRule";
    static final String MATCH_EXPRESSION = "target.alias=='someAlias'";
    static final String EVENT_SPECIFIER = "template=Something";

    Rule.Builder builder;

    @BeforeEach
    void setup() {
        builder = new Rule.Builder();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowOnBlankName(String s) {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.name(s)
                                    .matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier(EVENT_SPECIFIER)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"name\" cannot be blank, was \"" + s + "\""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowOnBlankMatchExpression(String s) {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.name(NAME)
                                    .matchExpression(s)
                                    .eventSpecifier(EVENT_SPECIFIER)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"matchExpression\" cannot be blank, was \"" + s + "\""));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "while (true) continue; false",
                "function foo() { return false; }; foo();",
                "System.exit(1)",
                "java.lang.System.exit(1)"
            })
    void shouldThrowOnInvalidMatchExpression(String s) {
        MatchExpressionValidationException ex =
                Assertions.assertThrows(
                        MatchExpressionValidationException.class,
                        () -> {
                            builder.name(NAME)
                                    .matchExpression(s)
                                    .eventSpecifier(EVENT_SPECIFIER)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getCause(), Matchers.instanceOf(IllegalMatchExpressionException.class));
        MatcherAssert.assertThat(
                ex.getCause().getMessage(),
                Matchers.startsWith("matchExpression rejected, illegal"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "events=incorrect", "tenplate=typo"})
    void shouldThrowOnInvalidEventSpecifier(String s) {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.name(NAME)
                                    .matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier(s)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.either(
                                Matchers.equalTo(
                                        "\"eventSpecifier\" cannot be blank, was \"" + s + "\""))
                        .or(Matchers.equalTo(s)));
    }

    @Test
    void shouldThrowOnNegativeArchivalPeriod() {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.name(NAME)
                                    .matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier(EVENT_SPECIFIER)
                                    .archivalPeriodSeconds(-1)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString(
                        "\"archivalPeriodSeconds\" cannot be negative, was \"-1\""));
    }

    @Test
    void shouldThrowOnNegativePreservedArchives() {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.name(NAME)
                                    .matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier(EVENT_SPECIFIER)
                                    .preservedArchives(-1)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"preservedArchives\" cannot be negative, was \"-1\""));
    }

    @Test
    void shouldDefaultToEmptyDescriptionIfLeftNull() throws Exception {
        Rule rule =
                builder.name(NAME)
                        .matchExpression(MATCH_EXPRESSION)
                        .eventSpecifier(EVENT_SPECIFIER)
                        .build();
        MatcherAssert.assertThat(rule.getDescription(), Matchers.is(Matchers.emptyString()));
    }

    @Test
    void shouldSanitizeName() throws Exception {
        Rule rule =
                builder.name("Some Rule")
                        .matchExpression(MATCH_EXPRESSION)
                        .eventSpecifier(EVENT_SPECIFIER)
                        .build();
        MatcherAssert.assertThat(rule.getName(), Matchers.equalTo("Some_Rule"));
    }

    @Test
    void shouldSanitizeRecordingNameAndMarkAsAutomatic() throws Exception {
        Rule rule =
                builder.name("Some Rule")
                        .matchExpression(MATCH_EXPRESSION)
                        .eventSpecifier(EVENT_SPECIFIER)
                        .build();
        MatcherAssert.assertThat(rule.getRecordingName(), Matchers.equalTo("auto_Some_Rule"));
    }

    @Test
    void shouldAcceptEventSpecifierArchiveSpecialCase() throws Exception {
        Rule rule = builder.matchExpression(MATCH_EXPRESSION).eventSpecifier("archive").build();
        MatcherAssert.assertThat(rule.getEventSpecifier(), Matchers.equalTo("archive"));
    }

    @Test
    void shouldAcceptEventSpecifierArchiveSpecialCaseWithName() throws Exception {
        Rule rule =
                builder.name("Some Rule")
                        .matchExpression(MATCH_EXPRESSION)
                        .eventSpecifier("archive")
                        .build();
        MatcherAssert.assertThat(rule.getEventSpecifier(), Matchers.equalTo("archive"));
    }

    @Test
    void shouldAcceptEventSpecifierArchiveSpecialCaseWithDescription() throws Exception {
        Rule rule =
                builder.description("Unused description")
                        .matchExpression(MATCH_EXPRESSION)
                        .eventSpecifier("archive")
                        .build();
        MatcherAssert.assertThat(rule.getEventSpecifier(), Matchers.equalTo("archive"));
    }

    @Test
    void shouldThrowOnArchiverWithArchivalPeriod() throws Exception {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier("archive")
                                    .archivalPeriodSeconds(5)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"archivalPeriodSeconds\" cannot be positive, was \"5\""));
    }

    @Test
    void shouldThrowOnArchiverWithPreservedArchives() throws Exception {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier("archive")
                                    .preservedArchives(5)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"preservedArchives\" cannot be positive, was \"5\""));
    }

    @Test
    void shouldThrowOnArchiverWithMaxSizeBytes() throws Exception {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier("archive")
                                    .maxSizeBytes(5)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"maxSizeBytes\" cannot be positive, was \"5\""));
    }

    @Test
    void shouldThrowOnArchiverWithMaxAgeSeconds() throws Exception {
        IllegalArgumentException ex =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            builder.matchExpression(MATCH_EXPRESSION)
                                    .eventSpecifier("archive")
                                    .maxAgeSeconds(5)
                                    .build();
                        });
        MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("\"maxAgeSeconds\" cannot be positive, was \"5\""));
    }

    @Nested
    class Deserialization {

        @Nested
        class Json {

            @Test
            void testCompleteRule()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String name = "Some Rule";
                String description = "This is a description";
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "template=Foo";
                int maxAgeSeconds = 60;
                int maxSizeBytes = 32 * 1024;
                int archivalPeriodSeconds = 300;
                int preservedArchives = 5;

                JsonObject json = new JsonObject();
                json.addProperty("name", name);
                json.addProperty("description", description);
                json.addProperty("matchExpression", matchExpression);
                json.addProperty("eventSpecifier", eventSpecifier);
                json.addProperty("maxAgeSeconds", maxAgeSeconds);
                json.addProperty("maxSizeBytes", maxSizeBytes);
                json.addProperty("archivalPeriodSeconds", archivalPeriodSeconds);
                json.addProperty("preservedArchives", preservedArchives);
                Rule rule = Rule.Builder.from(json).build();

                MatcherAssert.assertThat(rule.getName(), Matchers.equalTo("Some_Rule"));
                MatcherAssert.assertThat(rule.getDescription(), Matchers.equalTo(description));
                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
                MatcherAssert.assertThat(rule.getMaxAgeSeconds(), Matchers.equalTo(maxAgeSeconds));
                MatcherAssert.assertThat(rule.getMaxSizeBytes(), Matchers.equalTo(maxSizeBytes));
                MatcherAssert.assertThat(
                        rule.getArchivalPeriodSeconds(), Matchers.equalTo(archivalPeriodSeconds));
                MatcherAssert.assertThat(
                        rule.getPreservedArchives(), Matchers.equalTo(preservedArchives));
            }

            @Test
            void testRuleWithoutOptionalFields()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String name = "Some Rule";
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "template=Foo";

                JsonObject json = new JsonObject();
                json.addProperty("name", name);
                json.addProperty("matchExpression", matchExpression);
                json.addProperty("eventSpecifier", eventSpecifier);
                Rule rule = Rule.Builder.from(json).build();

                MatcherAssert.assertThat(rule.getName(), Matchers.equalTo("Some_Rule"));
                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
            }

            @Test
            void testArchiverWithoutName()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "archive";

                JsonObject json = new JsonObject();
                json.addProperty("matchExpression", matchExpression);
                json.addProperty("eventSpecifier", eventSpecifier);
                Rule rule = Rule.Builder.from(json).build();

                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
            }
        }

        @Nested
        class Form {

            @Test
            void testCompleteRule()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String name = "Some Rule";
                String description = "This is a description";
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "template=Foo";
                int maxAgeSeconds = 60;
                int maxSizeBytes = 32 * 1024;
                int archivalPeriodSeconds = 300;
                int preservedArchives = 5;

                MultiMap form = MultiMap.caseInsensitiveMultiMap();
                form.set("name", name);
                form.set("description", description);
                form.set("matchExpression", matchExpression);
                form.set("eventSpecifier", eventSpecifier);
                form.set("maxAgeSeconds", String.valueOf(maxAgeSeconds));
                form.set("maxSizeBytes", String.valueOf(maxSizeBytes));
                form.set("archivalPeriodSeconds", String.valueOf(archivalPeriodSeconds));
                form.set("preservedArchives", String.valueOf(preservedArchives));
                Rule rule = Rule.Builder.from(form).build();

                MatcherAssert.assertThat(rule.getName(), Matchers.equalTo("Some_Rule"));
                MatcherAssert.assertThat(rule.getDescription(), Matchers.equalTo(description));
                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
                MatcherAssert.assertThat(rule.getMaxAgeSeconds(), Matchers.equalTo(maxAgeSeconds));
                MatcherAssert.assertThat(rule.getMaxSizeBytes(), Matchers.equalTo(maxSizeBytes));
                MatcherAssert.assertThat(
                        rule.getArchivalPeriodSeconds(), Matchers.equalTo(archivalPeriodSeconds));
                MatcherAssert.assertThat(
                        rule.getPreservedArchives(), Matchers.equalTo(preservedArchives));
            }

            @Test
            void testRuleWithoutOptionalFields()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String name = "Some Rule";
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "template=Foo";

                MultiMap form = MultiMap.caseInsensitiveMultiMap();
                form.set("name", name);
                form.set("matchExpression", matchExpression);
                form.set("eventSpecifier", eventSpecifier);
                Rule rule = Rule.Builder.from(form).build();

                MatcherAssert.assertThat(rule.getName(), Matchers.equalTo("Some_Rule"));
                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
            }

            @Test
            void testArchiverWithoutName()
                    throws IllegalArgumentException, MatchExpressionValidationException {
                String matchExpression = "target.alias=='TheAlias'";
                String eventSpecifier = "archive";

                MultiMap form = MultiMap.caseInsensitiveMultiMap();
                form.set("matchExpression", matchExpression);
                form.set("eventSpecifier", eventSpecifier);
                Rule rule = Rule.Builder.from(form).build();

                MatcherAssert.assertThat(
                        rule.getMatchExpression(), Matchers.equalTo(matchExpression));
                MatcherAssert.assertThat(
                        rule.getEventSpecifier(), Matchers.equalTo(eventSpecifier));
            }
        }
    }
}
