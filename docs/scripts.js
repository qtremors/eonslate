// Eonslate website behavior. Loaded with `defer`, so the page is parsed before this runs.

const reduceMotionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

// Navbar tonal elevation once the page leaves the top. A sentinel observer replaces a scroll
// listener, so nothing runs while scrolling.
const navbar = document.getElementById('navbar');
if (navbar) {
    const navSentinel = document.createElement('div');
    navSentinel.setAttribute('aria-hidden', 'true');
    navSentinel.style.cssText = 'position:absolute;top:0;left:0;width:1px;height:20px;pointer-events:none';
    document.body.prepend(navSentinel);
    new IntersectionObserver(([entry]) => {
        navbar.classList.toggle('is-scrolled', !entry.isIntersecting);
    }).observe(navSentinel);
}

// Scroll reveal: each block fades up once, the first time it enters the screen
const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
    });
}, { threshold: 0.1 });
document.querySelectorAll('.reveal').forEach(el => revealObserver.observe(el));


// Mobile menu
const mobileMenuBtn = document.getElementById('mobile-menu-btn');
const mobileMenu = document.getElementById('mobile-menu');
if (mobileMenuBtn && mobileMenu) {
    const menuIcon = document.getElementById('mobile-menu-icon');

    const setMenuOpen = open => {
        mobileMenu.classList.toggle('is-open', open);
        mobileMenuBtn.setAttribute('aria-expanded', String(open));
        mobileMenuBtn.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
        menuIcon?.setAttribute('href', open ? '#i-x' : '#i-menu');
        document.body.style.overflow = open ? 'hidden' : '';
    };

    mobileMenuBtn.addEventListener('click', () => setMenuOpen(!mobileMenu.classList.contains('is-open')));
    document.querySelectorAll('.mobile-link').forEach(link => {
        link.addEventListener('click', () => setMenuOpen(false));
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && mobileMenu.classList.contains('is-open')) setMenuOpen(false);
    });
}

// Live GitHub stats and release downloads
const numberFormatter = new Intl.NumberFormat();

function animateCounter(element, target) {
    if (!element || !Number.isFinite(target)) return;

    const endValue = Math.max(0, Math.trunc(target));
    if (reduceMotionQuery.matches || endValue === 0) {
        element.innerText = numberFormatter.format(endValue);
        return;
    }

    const duration = 800;
    const startTime = performance.now();

    function updateCounter(currentTime) {
        const progress = Math.min((currentTime - startTime) / duration, 1);
        const easedProgress = 1 - Math.pow(1 - progress, 3);
        element.innerText = numberFormatter.format(Math.round(endValue * easedProgress));

        if (progress < 1) {
            requestAnimationFrame(updateCounter);
        }
    }

    requestAnimationFrame(updateCounter);
}

function sumReleaseDownloads(releases) {
    return releases.reduce((releaseTotal, release) => {
        const assetTotal = Array.isArray(release.assets)
            ? release.assets.reduce((total, asset) => total + (asset.download_count || 0), 0)
            : 0;
        return releaseTotal + assetTotal;
    }, 0);
}

async function fetchTotalReleaseDownloads() {
    let nextUrl = 'https://api.github.com/repos/qtremors/eonslate/releases?per_page=100';
    let totalDownloads = 0;

    while (nextUrl) {
        const response = await fetch(nextUrl);
        if (!response.ok) throw new Error(`GitHub releases request failed: ${response.status}`);

        const releases = await response.json();
        totalDownloads += sumReleaseDownloads(releases);

        const nextLink = response.headers.get('link')
            ?.split(',')
            .find(link => link.includes('rel="next"'));
        nextUrl = nextLink?.match(/<([^>]+)>/)?.[1] || '';
    }

    return totalDownloads;
}

async function fetchGitHubStats() {
    const [repoResult, releaseResult, totalDownloadsResult] = await Promise.allSettled([
        fetch('https://api.github.com/repos/qtremors/eonslate'),
        fetch('https://api.github.com/repos/qtremors/eonslate/releases/latest'),
        fetchTotalReleaseDownloads()
    ]);

    try {
        const repoRes = repoResult.status === 'fulfilled' ? repoResult.value : null;
        if (repoRes?.ok) {
            const data = await repoRes.json();
            if (data.stargazers_count !== undefined) {
                animateCounter(document.getElementById('gh-stars'), data.stargazers_count);
                animateCounter(document.getElementById('gh-forks'), data.forks_count);
            }
        }
    } catch (error) {
        console.error('Error fetching GitHub repository stats:', error);
    }

    try {
        const releaseRes = releaseResult.status === 'fulfilled' ? releaseResult.value : null;
        if (releaseRes?.ok) {
            const release = await releaseRes.json();
            if (release.tag_name) {
                document.querySelectorAll('.download-btn-text').forEach(el => {
                    el.innerText = `Download ${release.tag_name}`;
                });
            }
            const apk = (release.assets || []).find(asset => asset.name.toLowerCase().endsWith('.apk'));
            if (apk) {
                document.querySelectorAll('.download-link').forEach(link => {
                    link.href = apk.browser_download_url;
                });
            }
            animateCounter(document.getElementById('gh-latest-downloads'), sumReleaseDownloads([release]));
        }
    } catch (error) {
        console.error('Error fetching the latest GitHub release:', error);
    }

    if (totalDownloadsResult.status === 'fulfilled') {
        animateCounter(document.getElementById('gh-total-downloads'), totalDownloadsResult.value);
    } else {
        console.error('Error fetching total GitHub downloads:', totalDownloadsResult.reason);
    }
}

fetchGitHubStats();
