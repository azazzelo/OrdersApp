package com.example.ordersapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ordersapp.databinding.ActivityTablesBinding
import com.example.ordersapp.db.AppDatabase
import com.example.ordersapp.db.OrdersDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TablesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTablesBinding
    private lateinit var ordersDao: OrdersDao // DAO for analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTablesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OrdersDao
        ordersDao = AppDatabase.getDatabase(this).ordersDao()

        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val roleId = sharedPrefs.getInt("roleId", -1)

        // Setup button visibility and load analytics based on role
        setupUIForRole(roleId)

        // Setup navigation button listeners
        setupNavigationButtons()

        // Back button (logout) listener
        binding.btnTablesBack.setOnClickListener {
            navigateToAuth()
        }
    }

    private fun setupUIForRole(roleId: Int) {
        when (roleId) {
            1 -> { // Director
                setDirectorUI()
                loadAnalytics() // Director sees analytics
            }
            2 -> { // Dispatcher
                setDispatcherUI()
            }
            3 -> { // Accountant
                setAccountantUI()
                loadAnalytics() // Accountant sees analytics
            }
            else -> {
                Log.e("TablesActivity", "Unknown roleId: $roleId. Finishing activity.")
                finish() // Unknown role, close activity
            }
        }
    }

    private fun setDirectorUI() {
        // All buttons visible
        binding.btnTableRoles.visibility = View.VISIBLE
        binding.btnTableUsers.visibility = View.VISIBLE
        binding.btnTableCustomers.visibility = View.VISIBLE
        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableOrdersProducts.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
        // Analytics panel visible
        binding.analyticsPanel.visibility = View.VISIBLE
    }

    private fun setDispatcherUI() {
        // Access only to products, orders, customers
        binding.btnTableRoles.visibility = View.GONE
        binding.btnTableUsers.visibility = View.GONE
        binding.btnTableOrdersProducts.visibility = View.GONE
        binding.btnTableCustomers.visibility = View.VISIBLE
        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
        // Analytics panel hidden
        binding.analyticsPanel.visibility = View.GONE
    }

    private fun setAccountantUI() {
        // Access only to products and orders
        binding.btnTableRoles.visibility = View.GONE
        binding.btnTableUsers.visibility = View.GONE
        binding.btnTableCustomers.visibility = View.GONE
        binding.btnTableOrdersProducts.visibility = View.GONE
        binding.btnTableOrders.visibility = View.VISIBLE
        binding.btnTableProducts.visibility = View.VISIBLE
        // Analytics panel visible
        binding.analyticsPanel.visibility = View.VISIBLE
    }

    private fun setupNavigationButtons() {
        binding.btnTableRoles.setOnClickListener {
            startActivity(Intent(this, RolesEntityActivity::class.java))
        }
        binding.btnTableUsers.setOnClickListener {
            startActivity(Intent(this, UsersEntityActivity::class.java))
        }
        binding.btnTableCustomers.setOnClickListener {
            startActivity(Intent(this, CustomersEntityActivity::class.java))
        }
        binding.btnTableOrders.setOnClickListener {
            startActivity(Intent(this, OrdersEntityActivity::class.java))
        }
        binding.btnTableOrdersProducts.setOnClickListener {
            // Consider renaming or changing the purpose of this button,
            // as AddProductInOrderActivity is now called from OrdersRedActivity.
            // Keeping the navigation for now.
            startActivity(Intent(this, AddProductInOrderActivity::class.java))
        }
        binding.btnTableProducts.setOnClickListener {
            startActivity(Intent(this, ProductsEntityActivity::class.java))
        }
    }

    // Method to load and display analytics data
    private fun loadAnalytics() {
        lifecycleScope.launch {
            try {
                // 1. Get revenue for the current month
                val calendar = Calendar.getInstance()
                val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US) // Use US Locale for format consistency
                val currentYearMonth = yearMonthFormat.format(calendar.time)

                val monthlyRevenue = ordersDao.getMonthlyRevenue(currentYearMonth)
                // Use Locale.US for currency formatting consistency if needed, or Locale.getDefault()
                binding.tvMonthlyRevenue.text = String.format(Locale.US, "This Month's Revenue: $%.2f", monthlyRevenue ?: 0.0)
                Log.d("TablesActivity", "Monthly revenue for $currentYearMonth: $monthlyRevenue")

                // 2. Get top selling products for the current month
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfMonth = Calendar.getInstance().timeInMillis // End of current day is sufficient

                val topProducts = ordersDao.getTopSellingProducts(startOfMonth, endOfMonth, limit = 5) // Top 5

                if (topProducts.isNotEmpty()) {
                    // Format the string for display
                    val topProductsString = topProducts.mapIndexed { index, productInfo ->
                        "${index + 1}. ${productInfo.productName} (${productInfo.totalQuantitySold} units)"
                    }.joinToString("\n")
                    binding.tvTopSellingProducts.text = "Top Selling Products (Month):\n$topProductsString"
                    Log.d("TablesActivity", "Top products loaded: ${topProducts.size} items")
                } else {
                    binding.tvTopSellingProducts.text = "Top Selling Products (Month): No data"
                    Log.d("TablesActivity", "No top products found for the period.")
                }

            } catch (e: Exception) {
                Log.e("TablesActivity", "Error loading analytics data", e)
                // Display error messages
                binding.tvMonthlyRevenue.text = "Revenue: Error loading data"
                binding.tvTopSellingProducts.text = "Top Products: Error loading data"
            }
        }
    }

    private fun navigateToAuth() {
        // Clear authentication data
        getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply()

        // Navigate back to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}