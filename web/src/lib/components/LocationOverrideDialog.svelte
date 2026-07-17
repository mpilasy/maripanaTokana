<script lang="ts">
    import { setLocationOverride, resetLocationToCurrent } from '$lib/stores/devMode';

    let query = $state('');
    let results = $state<any[]>([]);
    let timeout: ReturnType<typeof setTimeout>;

    function handleInput() {
        clearTimeout(timeout);
        timeout = setTimeout(async () => {
            if (query.trim().length === 0) {
                results = [];
                return;
            }

            // check for coords
            const match = query.trim().match(/^(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)$/);
            if (match) {
                const lat = parseFloat(match[1]);
                const lon = parseFloat(match[2]);
                if (!isNaN(lat) && !isNaN(lon)) {
                    results = [{
                        id: 0,
                        name: `${lat}, ${lon}`,
                        latitude: lat,
                        longitude: lon,
                        displayName: 'Coordinates'
                    }];
                    return;
                }
            }

            // Open-Meteo's `name` param only matches the place name itself — a combined
            // "City, State" query returns zero results. Split off the qualifier (state/country)
            // and use it to filter the results client-side instead.
            const commaIndex = query.indexOf(',');
            const namePart = commaIndex >= 0 ? query.slice(0, commaIndex).trim() : query.trim();
            const qualifier = commaIndex >= 0 ? query.slice(commaIndex + 1).trim().toLowerCase() : '';

            try {
                const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(namePart)}&count=20&language=en&format=json`);
                const data = await res.json();
                if (data.results) {
                    let mapped = data.results.map((r: any) => ({
                        ...r,
                        displayName: [r.name, r.admin1, r.country].filter(Boolean).join(', ')
                    }));
                    if (qualifier) {
                        const filtered = mapped.filter((r: any) =>
                            r.admin1?.toLowerCase().includes(qualifier) ||
                            r.admin2?.toLowerCase().includes(qualifier) ||
                            r.country?.toLowerCase().includes(qualifier)
                        );
                        if (filtered.length > 0) mapped = filtered;
                    }
                    results = mapped;
                } else {
                    results = [];
                }
            } catch (e) {
                results = [];
            }
        }, 500);
    }

    function selectLocation(result: any) {
        const name = result.name;
        const subtext = [result.admin1, result.country].filter(Boolean).join(', ');
        setLocationOverride(result.latitude, result.longitude, name, subtext);
    }
</script>

<!-- svelte-ignore a11y_click_events_have_key_events -->
<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div class="scrim" role="button" tabindex="0" onclick={() => { resetLocationToCurrent(); }} onkeydown={(e) => { if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); resetLocationToCurrent(); } }}>
    <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title" aria-describedby="dialog-subtitle" tabindex="-1" onclick={(e) => e.stopPropagation()}>
        <h2 id="dialog-title" class="title">Developer Mode</h2>
        <h3 id="dialog-subtitle" class="subtitle">Override Location</h3>

        <div class="search-container">
            <input
                type="text"
                bind:value={query}
                oninput={handleInput}
                placeholder="Search city, zip, or lat,lon"
                class="search-input"
            />
            <button class="icon-button" onclick={() => resetLocationToCurrent()} title="My Location" aria-label="My Location">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
            </button>
        </div>

        <div class="results">
            {#each results as result}
                <button class="result-item" onclick={() => selectLocation(result)}>
                    <div class="result-name">{result.name}</div>
                    <div class="result-meta">{result.displayName}</div>
                </button>
            {/each}
        </div>
    </div>
</div>

<style>
    .scrim {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        padding: 24px;
    }

    .dialog {
        background: #0E0B3D;
        border-radius: 16px;
        padding: 24px;
        width: 100%;
        max-width: 400px;
        color: white;
    }

    .title {
        font-size: 20px;
        font-weight: 600;
        margin: 0;
    }

    .subtitle {
        font-size: 16px;
        color: rgba(255, 255, 255, 0.7);
        margin: 4px 0 16px 0;
        font-weight: 400;
    }

    .search-container {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
    }

    .search-input {
        flex: 1;
        padding: 12px 16px;
        border-radius: 8px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        background: transparent;
        color: white;
        font-size: 16px;
        box-sizing: border-box;
    }

    .search-input:focus {
        outline: none;
        border-color: rgba(255, 255, 255, 0.5);
    }

    .icon-button {
        background: transparent;
        border: none;
        color: white;
        cursor: pointer;
        padding: 8px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;
    }

    .icon-button:hover {
        background: rgba(255, 255, 255, 0.1);
    }

    .icon-button:focus-visible {
        outline: 2px solid rgba(255, 255, 255, 0.5);
        outline-offset: 2px;
    }

    .results {
        max-height: 200px;
        overflow-y: auto;
        margin-bottom: 16px;
    }

    .result-item {
        width: 100%;
        text-align: left;
        background: transparent;
        border: none;
        padding: 12px 8px;
        color: white;
        cursor: pointer;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .result-item:hover {
        background: rgba(255, 255, 255, 0.05);
    }

    .result-item:focus-visible {
        outline: 2px solid rgba(255, 255, 255, 0.5);
        outline-offset: 2px;
    }

    .result-name {
        font-size: 16px;
    }

    .result-meta {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.5);
    }
</style>
