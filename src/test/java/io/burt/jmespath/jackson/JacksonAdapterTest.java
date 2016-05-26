package io.burt.jmespath.jackson;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.burt.jmespath.AstGenerator;
import io.burt.jmespath.Query;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.contains;

public class JacksonAdapterTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode contact;
  private JsonNode cloudtrail;
  private JacksonAdapter adapter;

  private JsonNode loadExample(String path) {
    try {
      return objectMapper.readTree(getClass().getResource(path));
    } catch (IOException ioe) {
      fail(String.format("Failed parsing %s: \"%s\"", path, ioe.getMessage()));
      return null;
    }
  }

  private JsonNode parseString(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (IOException ioe) {
      fail(String.format("Failed parsing %s: \"%s\"", json, ioe.getMessage()));
      return null;
    }
  }

  private List<String> toStringList(JsonNode node) {
    List<String> strings = new ArrayList<>(node.size());
    for (JsonNode element : node) {
      strings.add(element.asText());
    }
    return strings;
  }

  private JsonNode evaluate(String query, JsonNode input) {
    return AstGenerator.fromString(query).evaluate(adapter, input);
  }

  @Before
  public void beforeEach() {
    contact = loadExample("/contact.json");
    cloudtrail = loadExample("/cloudtrail.json");
    adapter = new JacksonAdapter();
  }

  @Test
  public void topLevelProperty() {
    JsonNode result = evaluate("lastName", contact);
    assertThat(result.asText(), is("Smith"));
  }

  @Test
  public void chainProperty() {
    JsonNode result = evaluate("address.state", contact);
    assertThat(result.asText(), is("NY"));
  }

  @Test
  public void propertyNotFound() {
    JsonNode result = evaluate("address.country", contact);
    assertThat(result.isNull(), is(true));
  }

  @Test
  public void nullValue() {
    JsonNode result = evaluate("spouse", contact);
    assertThat(result.isNull(), is(true));
  }

  @Test
  public void index() {
    JsonNode result = evaluate("phoneNumbers[1].type", contact);
    assertThat(result.asText(), is("office"));
  }

  @Test
  public void negativeIndex() {
    JsonNode result = evaluate("phoneNumbers[-2].type", contact);
    assertThat(result.asText(), is("office"));
  }

  @Test
  public void indexNotFound() {
    JsonNode result = evaluate("phoneNumbers[3].type", contact);
    assertThat(result.isNull(), is(true));
  }

  @Test
  public void negativeIndexNotFound() {
    JsonNode result = evaluate("phoneNumbers[-4].type", contact);
    assertThat(result.isNull(), is(true));
  }

  @Test
  public void projection() {
    JsonNode result = evaluate("phoneNumbers[*].type", contact);
    assertThat(toStringList(result), contains("home", "office", "mobile"));
  }

  @Test
  public void multiStepProjection() {
    JsonNode result = evaluate("Records[*].userIdentity.userName", cloudtrail);
    assertThat(toStringList(result), contains("Alice", "Bob", "Alice"));
  }

  @Test
  public void projectionFiltersNull() {
    JsonNode result = evaluate("Records[*].requestParameters.keyName", cloudtrail);
    assertThat(toStringList(result), contains("mykeypair"));
  }

  @Test
  public void pipeStopsProjections() {
    JsonNode result = evaluate("Records[*].userIdentity | [1].userName", cloudtrail);
    assertThat(result.asText(), is("Bob"));
  }

  @Test
  public void literalString() {
    JsonNode result = evaluate("'hello world'", cloudtrail);
    assertThat(result.asText(), is("hello world"));
  }

  @Test
  public void literalStringIgnoresSource() {
    JsonNode result = evaluate("Records[*] | 'hello world'", cloudtrail);
    assertThat(result.asText(), is("hello world"));
  }

  public void flattenStartsProjection() {
    JsonNode result = evaluate("Records[].userIdentity.userName", cloudtrail);
    assertThat(toStringList(result), contains("Alice", "Bob", "Alice"));
  }

  @Test
  public void flattenArray() {
    JsonNode nestedArray = parseString("[[0, 1, 2]]");
    JsonNode result = evaluate("[]", nestedArray);
    assertThat(result, is(parseString("[0, 1, 2]")));
  }

  @Test
  public void flattenNonArrayProducesNull() {
    JsonNode result = evaluate("Records[0].userIdentity.userName[]", cloudtrail);
    assertThat(result.isNull(), is(true));
  }

  @Test
  public void flattenMultipleTimes() {
    JsonNode nestedArray = parseString("[[0, 1, 2]]");
    JsonNode result = evaluate("[][][][][][][][][][][][][]", nestedArray);
    assertThat(result, is(parseString("[0, 1, 2]")));
  }

  @Test
  public void flattenInProjection() {
    JsonNode nestedArray = parseString("[{\"a\":[0]},{\"a\":[1]}]");
    JsonNode result = evaluate("[*].a[]", nestedArray);
    assertThat(result, is(parseString("[0, 1]")));
  }
}
