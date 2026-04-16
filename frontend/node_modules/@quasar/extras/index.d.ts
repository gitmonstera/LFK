import type {
  generalAnimations,
  inAnimations,
  outAnimations
} from "./animate/animate-list.d.ts";

export type QuasarFonts = "roboto-font" | "roboto-font-latin-ext";

export type QuasarGeneralAnimations = generalAnimations;
export type QuasarInAnimations = inAnimations;
export type QuasarOutAnimations = outAnimations;
export type QuasarAnimations =
  | QuasarGeneralAnimations
  | QuasarInAnimations
  | QuasarOutAnimations;

export type QuasarExtrasIcons =
  | "bootstrap-icons"
  | "eva-icons"
  | "fontawesome-v5"
  | "fontawesome-v5-pro"
  | "fontawesome-v6"
  | "fontawesome-v6-pro"
  | "fontawesome-v7"
  | "fontawesome-v7-pro"
  | "ionicons-v4"
  | "line-awesome"
  | "material-icons"
  | "material-icons-outlined"
  | "material-icons-round"
  | "material-icons-sharp"
  | "material-symbols-outlined"
  | "material-symbols-rounded"
  | "material-symbols-sharp"
  | "mdi-v3"
  | "mdi-v4"
  | "mdi-v5"
  | "mdi-v6"
  | "mdi-v7"
  | "themify"
  | "svg-bootstrap-icons"
  | "svg-eva-icons"
  | "svg-fontawesome-v5"
  | "svg-fontawesome-v6"
  | "svg-fontawesome-v7"
  | "svg-ionicons-v4"
  | "svg-ionicons-v5"
  | "svg-ionicons-v6"
  | "svg-ionicons-v7"
  | "svg-ionicons-v8"
  | "svg-line-awesome"
  | "svg-material-icons"
  | "svg-material-icons-outlined"
  | "svg-material-icons-round"
  | "svg-material-icons-sharp"
  | "svg-material-symbols-outlined"
  | "svg-material-symbols-rounded"
  | "svg-material-symbols-sharp"
  | "svg-mdi-v4"
  | "svg-mdi-v5"
  | "svg-mdi-v6"
  | "svg-mdi-v7"
  | "svg-themify";
