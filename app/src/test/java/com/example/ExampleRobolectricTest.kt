package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameEngine
import com.example.game.GameEnvironment
import com.example.game.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Political Run", appName)
  }

  @Test
  fun `game engine initial state and lane movement`() {
    val engine = GameEngine()
    engine.startNewGame()

    assertTrue(engine.isRunning)
    assertFalse(engine.isGameOver)
    assertEquals(0, engine.targetLane)

    // Move left
    engine.moveLeft()
    assertEquals(-1, engine.targetLane)

    // Cannot move further left than -1
    engine.moveLeft()
    assertEquals(-1, engine.targetLane)

    // Move right twice
    engine.moveRight()
    assertEquals(0, engine.targetLane)
    engine.moveRight()
    assertEquals(1, engine.targetLane)

    // Cannot move beyond 1
    engine.moveRight()
    assertEquals(1, engine.targetLane)
  }

  @Test
  fun `game engine jump and slide state`() {
    val engine = GameEngine()
    engine.startNewGame()

    // Jump
    engine.jump()
    assertTrue(engine.isJumping)
    assertFalse(engine.isSliding)

    // Slide
    engine.slide()
    assertTrue(engine.isSliding)
  }

  @Test
  fun `environment changes with score progression`() {
    assertEquals(GameEnvironment.INDIAN_CITY, GameEnvironment.forScore(500))
    assertEquals(GameEnvironment.VILLAGE, GameEnvironment.forScore(2000))
    assertEquals(GameEnvironment.MOUNTAIN_ROAD, GameEnvironment.forScore(4500))
    assertEquals(GameEnvironment.FOREST, GameEnvironment.forScore(7000))
    assertEquals(GameEnvironment.NIGHT_CITY, GameEnvironment.forScore(12000))
  }
}

