package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FilterDatabase
import com.example.data.FilterRepository
import com.example.dsp.FilterMethod
import com.example.dsp.FilterType
import com.example.ui.viewmodel.FilterViewModel
import org.junit.Assert.assertEquals
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
    assertEquals("Filter Designer", appName)
  }

  @Test
  fun testViewModelAndDSPCalculation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = FilterDatabase.getDatabase(context)
    val repository = FilterRepository(database.filterDao())
    val viewModel = FilterViewModel(repository)
    
    // Exercise all filter designs
    for (method in FilterMethod.values()) {
      for (type in FilterType.values()) {
        viewModel.setFilterMethod(method)
        viewModel.setFilterType(type)
        val err = viewModel.errorMsg.value
        assert(err == null) { "Calculations failed for $method and $type with message: $err" }
      }
    }
  }
}
