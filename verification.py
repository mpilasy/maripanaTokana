from playwright.sync_api import sync_playwright

def verify_button_accessibility():
    with sync_playwright() as p:
        # Create a mock location context so the weather loads successfully
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            geolocation={"latitude": 40.7128, "longitude": -74.0060},
            permissions=["geolocation"]
        )
        page = context.new_page()

        # Navigate to the local preview server
        page.goto("http://localhost:4173/")

        # Wait for the main weather screen to load successfully
        page.wait_for_selector('.hero-card', timeout=15000)

        page.keyboard.press("Tab") # Focus Aa controls button

        # Take a screenshot to verify semantic buttons and focus-visible state
        page.screenshot(path="/home/jules/verification_focus_font.png")

        page.keyboard.press("Tab") # Focus language controls button
        page.screenshot(path="/home/jules/verification_focus_lang.png")

        page.keyboard.press("Tab") # Focus updated refresh button
        page.screenshot(path="/home/jules/verification_focus_refresh.png")

        context.close()
        browser.close()

if __name__ == "__main__":
    verify_button_accessibility()
