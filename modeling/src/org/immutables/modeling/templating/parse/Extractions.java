package org.immutables.modeling.templating.parse;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.parboiled.Action;
import org.parboiled.Context;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.support.Position;

public final class Extractions {
  private Extractions() {}

  public interface Extractor<T> {
    T get(Context<Object> context);
  }

  public interface Applicator extends Action<Object> {}

  public static <T> Extractor<T> value(final T value) {
    return new Extractor<T>() {
      @Override
      public T get(Context<Object> context) {
        return value;
      }
    };
  }

  public static Extractor<String> matched() {
    return MatchExtractor.INSTANCE;
  }

  public static Extractor<Position> positioned() {
    return PositionExtractor.INSTANCE;
  }

  // unchecked: will always work with any kind of object
  @SuppressWarnings("unchecked")
  public static <T> Extractor<T> popped() {
    return (Extractor<T>) PopingExtractor.INSTANCE;
  }

  public static <F, T> Extractor<T> popped(final Function<F, T> transform) {
    return new Extractor<T>() {
      @Override
      public T get(Context<Object> context) {
        return transform.apply(Extractions.<F>popped().get(context));
      }
    };
  }

  private enum MatchExtractor implements Extractor<String> {
    INSTANCE;
    @Override
    public String get(Context<Object> context) {
      return context.getMatch();
    }
  }

  private enum PositionExtractor implements Extractor<Position> {
    INSTANCE;
    @Override
    public Position get(Context<Object> context) {
      return context.getPosition();
    }
  }

  private enum PopingExtractor implements Extractor<Object>, Applicator {
    INSTANCE;
    @Override
    public Object get(Context<Object> context) {
      return context.getValueStack().pop();
    }

    @Override
    public boolean run(Context<Object> context) {
      get(context);
      return true;
    }
  }

  public static abstract class Instance<T> extends ExtractorApplicator<T> {
    @Override
    public T get(Context<Object> context) {
      return get();
    }

    public abstract T get();
  }

  static abstract class ExtractorApplicator<T> implements Extractor<T>, Applicator {
    @Override
    public final boolean run(Context<Object> context) {
      context.getValueStack().push(get(context));
      return true;
    }
  }

  public static abstract class Construct<T, V> extends ExtractorApplicator<T> {

    private final Extractor<? extends V> extractor;

    protected Construct(Extractor<? extends V> extractor) {
      this.extractor = extractor;
    }

    @Override
    public final T get(Context<Object> context) {
      V value = extractor.get(context);
      return get(value);
    }

    public abstract T get(V value);
  }

  public static Applicator diagnose() {
    return new Applicator() {
      @Override
      public boolean run(Context<Object> context) {
        printWhere(this, context);
        return true;
      }

      @Override
      public String toString() {
        return "diagnose()";
      }
    };
  }

  public static abstract class Specify<B, V> implements Applicator {

    private final Extractor<? extends V> extractor;

    protected Specify(Extractor<? extends V> extractor) {
      this.extractor = extractor;
    }

    @Override
    public final boolean run(Context<Object> context) {
      V value = extractor.get(context);
      B builder = (B) context.getValueStack().peek();
      specify(builder, value);
      return true;
    }

    public abstract void specify(B builder, V value);
  }

  public static abstract class Build<B, T> extends ExtractorApplicator<T> {
    @Override
    public final T get(Context<Object> context) {
      B builder = (B) context.getValueStack().pop();
      return build(builder);
    }

    public abstract T build(B builder);
  }

  public static abstract class Builder<B> implements Applicator {
    @Override
    public final boolean run(Context<Object> context) {
      B builder = builder();
      context.getValueStack().push(builder);
      return true;
    }

    public abstract B builder();
  }

  private static void printWhere(Object caller, Context<Object> context) {
    String message = ErrorUtils.printErrorMessage("%s:%s:%s",
        "",
        context.getCurrentIndex(),
        context.getCurrentIndex() + 2,
        context.getInputBuffer());
    System.err.println("*** " + caller + message + stack(context));
  }

  private static String stack(Context<Object> context) {
    return "\n\t" + Joiner.on("\n\t - ").join(context.getValueStack());
  }
}