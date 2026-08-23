package com.listen.expensetracker.data.i18n

import com.listen.arch.i18n.tr

/**
 * Type-safe Internationalization String Constants for ListenExpenseTracker.
 * Allows idiomatic, Flutter-like usage across the app: `AppStrings.app_version_label.tr(lang)`.
 */
object AppStrings {
    // Navigation
    const val nav_transactions = "nav_transactions"
    const val nav_statistics = "nav_statistics"
    const val nav_settings = "nav_settings"

    // Balance & Ledger
    const val balance_title = "balance_title"
    const val total_expense = "total_expense"
    const val total_income = "total_income"
    const val monthly_budget = "monthly_budget"
    const val used_budget = "used_budget"
    const val over_budget = "over_budget"
    const val search_placeholder = "search_placeholder"

    // Accounts & Filters
    const val filter_all = "filter_all"
    const val filter_wechat = "filter_wechat"
    const val filter_alipay = "filter_alipay"
    const val filter_bank = "filter_bank"
    const val filter_credit = "filter_credit"
    const val filter_cash = "filter_cash"
    const val sort_date_desc = "sort_date_desc"
    const val sort_date_asc = "sort_date_asc"
    const val sort_amount_desc = "sort_amount_desc"
    const val sort_amount_asc = "sort_amount_asc"

    // Statistics
    const val stats_title = "stats_title"
    const val tab_expense_analysis = "tab_expense_analysis"
    const val tab_income_analysis = "tab_income_analysis"
    const val daily_average_expense = "daily_average_expense"
    const val daily_average_income = "daily_average_income"
    const val max_expense = "max_expense"
    const val max_income = "max_income"
    const val trend_7days = "trend_7days"
    const val expense_ranking = "expense_ranking"
    const val income_ranking = "income_ranking"
    const val empty_transactions = "empty_transactions"
    const val empty_month_expense = "empty_month_expense"
    const val empty_month_income = "empty_month_income"

    // Buttons & Form Actions
    const val btn_add_transaction = "btn_add_transaction"
    const val btn_done = "btn_done"
    const val btn_save = "btn_save"
    const val btn_cancel = "btn_cancel"
    const val btn_delete = "btn_delete"
    const val type_expense = "type_expense"
    const val type_income = "type_income"
    const val common_confirm = "common_confirm"
    const val common_cancel = "common_cancel"
    const val common_save = "common_save"
    const val common_done = "common_done"
    const val common_delete = "common_delete"

    // Settings & Dialogs
    const val settings_title = "settings_title"
    const val settings_cloud = "settings_cloud"
    const val settings_appearance = "settings_appearance"
    const val settings_theme_mode = "settings_theme_mode"
    const val settings_accent_color = "settings_accent_color"
    const val settings_language = "settings_language"
    const val settings_currency = "settings_currency"
    const val settings_budget = "settings_budget"
    const val settings_category_manage = "settings_category_manage"
    const val settings_data_manage = "settings_data_manage"
    const val settings_system_ops = "settings_system_ops"
    const val theme_light = "theme_light"
    const val theme_dark = "theme_dark"
    const val theme_system = "theme_system"
    const val apm_inspector = "apm_inspector"
    const val seed_data_btn = "seed_data_btn"
    const val seed_demo = "seed_demo"
    const val clear_all = "clear_all"
    const val export_json = "export_json"
    const val export_csv = "export_csv"
    const val import_json = "import_json"

    // Cloud & Google Auth
    const val cloud_status_idle = "cloud_status_idle"
    const val cloud_status_syncing = "cloud_status_syncing"
    const val cloud_status_success = "cloud_status_success"
    const val cloud_status_error = "cloud_status_error"
    const val cloud_last_sync = "cloud_last_sync"
    const val cloud_backup_btn = "cloud_backup_btn"
    const val cloud_restore_btn = "cloud_restore_btn"
    const val google_account = "google_account"
    const val google_login_required = "google_login_required"
    const val google_login_btn = "google_login_btn"
    const val google_logout_btn = "google_logout_btn"
    const val google_logged_in = "google_logged_in"
    const val google_link_title = "google_link_title"
    const val google_link_desc = "google_link_desc"
    const val google_manual_email = "google_manual_email"
    const val google_email_placeholder = "google_email_placeholder"

    // About App
    const val about_app = "about_app"
    const val check_update = "check_update"
    const val check_update_desc = "check_update_desc"
    const val app_version_label = "app_version_label"
    const val app_architecture_label = "app_architecture_label"
    const val app_core_sdk_label = "app_core_sdk_label"
    const val app_features_label = "app_features_label"
    const val app_features_desc = "app_features_desc"

    // Dialogs & Prompts
    const val manage_accounts_title = "manage_accounts_title"
    const val add_account = "add_account"
    const val account_name_input = "account_name_input"
    const val select_month_dialog = "select_month_dialog"
    const val currency_dialog_title = "currency_dialog_title"
    const val currency_current = "currency_current"
    const val edit_transaction_title = "edit_transaction_title"
    const val budget_dialog_title = "budget_dialog_title"
    const val confirm_clear_title = "confirm_clear_title"
    const val confirm_clear_desc = "confirm_clear_desc"

    // Toast & Snackbar Messages
    const val backup_success_toast = "backup_success_toast"
    const val backup_failed_toast = "backup_failed_toast"
    const val restore_success_toast = "restore_success_toast"
    const val restore_empty_toast = "restore_empty_toast"
    const val restore_failed_toast = "restore_failed_toast"
    const val login_google_required_toast = "login_google_required_toast"
    const val seed_data_success_toast = "seed_data_success_toast"
    const val clear_all_success_toast = "clear_all_success_toast"
    const val undo_delete_toast = "undo_delete_toast"
    const val undo_action_label = "undo_action_label"
    const val undo_success_toast = "undo_success_toast"

    // Categories
    const val cat_food = "cat_food"
    const val cat_transport = "cat_transport"
    const val cat_shopping = "cat_shopping"
    const val cat_entertainment = "cat_entertainment"
    const val cat_housing = "cat_housing"
    const val cat_medical = "cat_medical"
    const val cat_social = "cat_social"
    const val cat_pets = "cat_pets"
    const val cat_fitness = "cat_fitness"
    const val cat_cafe = "cat_cafe"
    const val cat_other_exp = "cat_other_exp"
    const val cat_salary = "cat_salary"
    const val cat_investment = "cat_investment"
    const val cat_gift = "cat_gift"
    const val cat_other_inc = "cat_other_inc"
}
