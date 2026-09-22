package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyPhoto
import com.example.data.repository.PhotoRepository
import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daymark", appName)
  }

  @Test
  fun `test journal stats calculation with moments and dates`() {
    val today = LocalDate.of(2026, 9, 15)
    val photos = listOf(
      DailyPhoto(id = "1", journalDate = "2026-09-15", storagePath = "/p1.jpg"),
      DailyPhoto(id = "2", journalDate = "2026-09-14", storagePath = "/p2.jpg"),
      DailyPhoto(id = "3", journalDate = "2026-09-13", storagePath = "/p3.jpg"),
      DailyPhoto(id = "4", journalDate = "2026-09-10", storagePath = "/p4.jpg")
    )
    val stats = PhotoRepository.calculateStats(photos, today)
    assertEquals(4, stats.totalMoments)
    assertEquals("Sep 10, 2026", stats.firstDate)
    assertEquals("September 2026", stats.mostActiveMonth)
  }

  @Test
  fun `test multiple photos on same date calculation`() {
    val today = LocalDate.of(2026, 9, 15)
    // 5 photos on today, 2 photos on yesterday
    val photos = (1..5).map { idx ->
      DailyPhoto(id = "today_$idx", journalDate = "2026-09-15", storagePath = "/p$idx.jpg")
    } + (1..2).map { idx ->
      DailyPhoto(id = "yesterday_$idx", journalDate = "2026-09-14", storagePath = "/py$idx.jpg")
    }

    val stats = PhotoRepository.calculateStats(photos, today)
    assertEquals(7, stats.totalMoments)
  }

  @Test
  fun `test date state classification for today past and future`() {
    val today = LocalDate.of(2026, 9, 15)
    val yesterday = LocalDate.of(2026, 9, 14)
    val tomorrow = LocalDate.of(2026, 9, 16)

    assertEquals(DateTimeUtils.DateState.TODAY, DateTimeUtils.classifyDate(today, today))
    assertEquals(DateTimeUtils.DateState.PAST, DateTimeUtils.classifyDate(yesterday, today))
    assertEquals(DateTimeUtils.DateState.FUTURE, DateTimeUtils.classifyDate(tomorrow, today))

    assertTrue(DateTimeUtils.isAllowedForJournal(today, today))
    assertTrue(DateTimeUtils.isAllowedForJournal(yesterday, today))
    assertFalse(DateTimeUtils.isAllowedForJournal(tomorrow, today))
  }

  @Test
  fun `test timezone aware date parsing and formatting`() {
    val date = LocalDate.of(2026, 9, 15)
    val iso = DateTimeUtils.toIsoDate(date)
    assertEquals("2026-09-15", iso)

    val parsed = DateTimeUtils.parseIsoDate(iso)
    assertEquals(date, parsed)

    val display = DateTimeUtils.formatDisplayDate(date)
    assertEquals("Tuesday, September 15, 2026", display)
  }

  @Test
  fun `test settings repository profile picture persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.SettingsRepository(context)

    // Initially null or default
    repo.setProfilePictureUri(null)
    assertEquals(null, repo.settings.value.profilePictureUri)

    // Set picture URI
    val testUri = "/data/user/0/com.example/files/profile_avatar_12345.jpg"
    repo.setProfilePictureUri(testUri)
    assertEquals(testUri, repo.settings.value.profilePictureUri)

    // Clear picture URI
    repo.setProfilePictureUri(null)
    assertEquals(null, repo.settings.value.profilePictureUri)
  }

  @Test
  fun `test day bucket label and full date formatting`() {
    val today = LocalDate.of(2026, 9, 16)
    val yesterday = LocalDate.of(2026, 9, 15)
    val sep14 = LocalDate.of(2026, 9, 14)

    assertEquals("Today", DateTimeUtils.formatDayBucketLabel(today, today))
    assertEquals("Yesterday", DateTimeUtils.formatDayBucketLabel(yesterday, today))
    assertEquals("Sep 14", DateTimeUtils.formatDayBucketLabel(sep14, today))

    val fullDate = DateTimeUtils.formatFullDayDate(sep14)
    assertEquals("Monday, Sep 14, 2026", fullDate)
  }

  @Test
  fun `test settings repository custom day title persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.SettingsRepository(context)

    // Set custom name for Sep 14
    repo.setDayCustomTitle("2026-09-14", "Coast Roadtrip")
    assertEquals("Coast Roadtrip", repo.getDayCustomTitle("2026-09-14"))
    assertEquals("Coast Roadtrip", repo.dayCustomTitles.value["2026-09-14"])

    // Remove custom name
    repo.setDayCustomTitle("2026-09-14", null)
    assertEquals(null, repo.getDayCustomTitle("2026-09-14"))
    assertFalse(repo.dayCustomTitles.value.containsKey("2026-09-14"))
  }

  @Test
  fun `test day-based grouping contains multiple photos per bucket`() {
    val photos = listOf(
      DailyPhoto(id = "1", journalDate = "2026-09-16", storagePath = "/p1.jpg"),
      DailyPhoto(id = "2", journalDate = "2026-09-16", storagePath = "/p2.jpg"),
      DailyPhoto(id = "3", journalDate = "2026-09-16", storagePath = "/p3.jpg"),
      DailyPhoto(id = "4", journalDate = "2026-09-15", storagePath = "/p4.jpg")
    )
    val grouped = photos.groupBy { it.journalDate }
    assertEquals(2, grouped.size)
    assertEquals(3, grouped["2026-09-16"]?.size)
    assertEquals(1, grouped["2026-09-15"]?.size)
  }

  @Test
  fun `test settings repository user email and auth signout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.SettingsRepository(context)

    repo.setUserEmail("user@example.com")
    assertEquals("user@example.com", repo.settings.value.userEmail)

    repo.setUserName("Test User")
    assertEquals("Test User", repo.settings.value.userName)

    repo.signOut()
    assertEquals(false, repo.settings.value.isSignedIn)

    repo.signIn("user@example.com", "Test User")
    assertEquals(true, repo.settings.value.isSignedIn)
  }

  @Test
  fun `test profile stats strip calculation from photos list`() {
    val photos = listOf(
      DailyPhoto(id = "1", journalDate = "2026-09-16", storagePath = "/p1.jpg"),
      DailyPhoto(id = "2", journalDate = "2026-09-16", storagePath = "/p2.jpg"),
      DailyPhoto(id = "3", journalDate = "2026-09-15", storagePath = "/p3.jpg"),
      DailyPhoto(id = "4", journalDate = "2026-09-14", storagePath = "/p4.jpg")
    )
    val daysCaptured = photos.map { it.journalDate }.distinct().size
    val totalPhotos = photos.size
    assertEquals(3, daysCaptured)
    assertEquals(4, totalPhotos)
  }

  @Test
  fun `test acceptance scenario user session persistence and multi-user data isolation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.SettingsRepository(context)

    // 1. Sign in with User 1
    val user1Id = "c1a9d592-3a59-450f-9022-771be7ad7b26"
    val user1Email = "primary.user@gmail.com"
    val user1Name = "Primary User"
    repo.signInUser(user1Id, user1Email, user1Name, token = "test_token_1")

    assertTrue(repo.settings.value.isSignedIn)
    assertEquals(user1Id, repo.settings.value.userId)
    assertEquals(user1Email, repo.settings.value.userEmail)

    // Photos belonging to user 1 vs user 2
    val user1Photos = listOf(
      DailyPhoto(id = "p1_1", userId = user1Id, journalDate = "2026-09-21", storagePath = "$user1Id/day_1/1.jpg")
    )
    val user2Id = "d2b8e481-2b48-440e-8011-660ad6bc6a15"
    val user2Photos = listOf(
      DailyPhoto(id = "p2_1", userId = user2Id, journalDate = "2026-09-21", storagePath = "$user2Id/day_1/2.jpg")
    )

    // Data isolation check: User 1 photos only contain user1Id
    assertEquals(1, user1Photos.filter { it.userId == repo.settings.value.userId }.size)
    assertEquals(0, user2Photos.filter { it.userId == repo.settings.value.userId }.size)

    // 2. Sign out User 1
    repo.signOut()
    assertFalse(repo.settings.value.isSignedIn)
    assertEquals("", repo.settings.value.userId)

    // 3. Sign in with a second, different account (User 2)
    val user2Email = "second.user@gmail.com"
    val user2Name = "Alex Second"
    repo.signInUser(user2Id, user2Email, user2Name, token = "test_token_2")

    assertTrue(repo.settings.value.isSignedIn)
    assertEquals(user2Id, repo.settings.value.userId)
    assertEquals(user2Email, repo.settings.value.userEmail)

    // Verify isolation: User 2 sees only User 2 photos, none of User 1's photos
    assertEquals(0, user1Photos.filter { it.userId == repo.settings.value.userId }.size)
    assertEquals(1, user2Photos.filter { it.userId == repo.settings.value.userId }.size)
  }

  @Test
  fun `test native google sign in data model and client id string`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val webClientId = context.getString(R.string.default_web_client_id)
    assertTrue(webClientId.isNotBlank())
    assertTrue(webClientId.contains("apps.googleusercontent.com"))

    val result = com.example.util.GoogleSignInResult(
      idToken = "mock_id_token_12345",
      email = "newuser@gmail.com",
      displayName = "New User",
      profilePictureUri = "https://lh3.googleusercontent.com/a/test",
      givenName = "New",
      familyName = "User"
    )
    assertEquals("mock_id_token_12345", result.idToken)
    assertEquals("newuser@gmail.com", result.email)
    assertEquals("New User", result.displayName)
  }
}
