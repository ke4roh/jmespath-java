package io.burt.jmespath.node;

import io.burt.jmespath.Adapter;

public class ParsedJsonLiteralNode<T> extends JsonLiteralNode<T> {
  private final Object tree;

  public ParsedJsonLiteralNode(Adapter<T> runtime, String raw, Object tree) {
    super(runtime, raw);
    this.tree = tree;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T search(T input) {
    return (T) tree();
  }

  protected Object tree() {
    return tree;
  }

  @Override
  protected String internalToString() {
    return String.format("%s", tree());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean internalEquals(Object o) {
    if (o instanceof ParsedJsonLiteralNode) {
      ParsedJsonLiteralNode<T> other = (ParsedJsonLiteralNode<T>) o;
      return tree().equals(other.tree());
    } else {
      return super.internalEquals(o);
    }
  }

  @Override
  protected int internalHashCode() {
    int h = 1;
    h = h * 31 + tree().hashCode();
    return h;
  }
}
