package com.ververica.statefun.workshop.merchants;

import java.util.Arrays;

public class Merchants {

  private static String[] PREFIXES = {
    "active", "arc", "auto", "app", "avi", "base", "co", "con", "core", "clear", "en", "echo",
    "even", "ever", "fair", "go", "high", "hyper", "in", "inter", "iso", "jump", "live", "make",
    "mass", "meta", "matter", "omni", "on", "one", "open", "over", "out", "re", "real", "peak",
    "pure", "shift", "silver", "solid", "spark", "start", "true", "up", "vibe"
  };

  private static String[] WORD_SUFFIXES = {
    "arc", "atlas", "base", "bay", "boost", "capsule", "case", "center", "cast", "click", "dash",
    "deck", "dock", "dot", "drop", "engine", "flow", "glow", "grid", "gram", "graph", "hub",
    "focus", "kit", "lab", "level", "layer", "light", "line", "logic", "load", "loop", "ment",
    "method", "mode", "mark", "ness", "now", "pass", "port", "post", "press", "prime", "push",
    "rise", "scape", "scale", "scan", "scout", "sense", "set", "shift", "ship", "side", "signal",
    "snap", "scope", "space", "span", "spark", "spot", "start", "storm", "stripe", "sync", "tap",
    "tilt", "ture", "type", "view", "verge", "vibe", "ware", "yard", "up"
  };

  public static final String[] MERCHANTS =
      Arrays.stream(PREFIXES)
          .flatMap(prefix -> Arrays.stream(WORD_SUFFIXES).map(suffix -> prefix + suffix))
          .map(Merchants::capitalize)
          .toArray(String[]::new);

  private static String capitalize(String word) {
    return word.substring(0, 1).toUpperCase() + word.substring(1);
  }
}
