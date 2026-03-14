from playwright.sync_api import sync_playwright

def test_app():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Mock geolocation
        context = browser.new_context(
            geolocation={"latitude": 37.7749, "longitude": -122.4194},
            permissions=["geolocation"]
        )
        page = context.new_page()
        page.goto("http://localhost:5173")
        page.wait_for_timeout(5000) # wait for weather load
        page.screenshot(path="frontend_screenshot.png")
        browser.close()

if __name__ == "__main__":
    test_app()
