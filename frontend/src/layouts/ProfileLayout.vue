<template>
  <q-layout view="hHh Lpr lFf" class="profile-layout">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round dense icon="arrow_back" to="/" />

        <q-toolbar-title class="row items-center">
          <q-avatar size="32px" class="q-mr-sm">
            <img src="/logo/logo.svg" alt="LFK">
          </q-avatar>
          <span>Личный кабинет</span>
        </q-toolbar-title>

        <q-btn flat round dense icon="notifications">
          <q-badge color="red" floating>3</q-badge>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
// Логика для профиля
</script>

<style lang="scss" scoped>
.profile-layout {
  background: #f5f5f5;
  min-height: 100vh;
}
</style>
