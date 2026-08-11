import { defineConfig } from "deepsec/config";

export default defineConfig({
  projects: [
    {
      id: "squaretool",
      root: "..",
      promptAppend:
        "Prioritize backup import validation and rollback, Room database integrity, exported Android components, FileProvider and URI grants, generated file sharing, and sensitive logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/xml/",
        "app/src/main/java/com/finnvek/squaretool/backup/",
        "app/src/main/java/com/finnvek/squaretool/export/",
        "app/src/main/java/com/finnvek/squaretool/data/local/",
        "app/src/main/java/com/finnvek/squaretool/data/repository/",
        "app/src/main/java/com/finnvek/squaretool/ui/settings/"
      ]
    }
  ]
});
