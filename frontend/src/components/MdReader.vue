<script setup lang="ts">
import gfm from "@bytemd/plugin-gfm";
import highlight from "@bytemd/plugin-highlight";
import { Editor, Viewer } from "@bytemd/vue-next";
import { Editor } from "@bytemd/vue-next";
import { ref } from "vue";

const plugins = [
  @@ -21,4 +21,4 @@ const handleChange = (v: string) => {
};
</script>

<style scoped></style>
<style scoped></style>