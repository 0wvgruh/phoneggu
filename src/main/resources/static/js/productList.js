document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const category = urlParams.get('category');
    const type = urlParams.get('type');

    if (category && type) {
        updateTopLeft(category, type);
    }

    document.querySelectorAll('.nav-link').forEach(function(link) {
        link.addEventListener('click', function(e) {
            // e.preventDefault(); // Preventing default behavior is no longer needed
            const category = link.getAttribute('data-category');
            const type = link.getAttribute('data-type');
            updateTopLeft(category, type);
        });
    });

    function updateTopLeft(mainCategory, subCategory) {
        const mainCategoryMap = {
            PHONE_CASE: '폰 케이스',
            TOK: '톡',
            AIRPODS: '에어팟/버즈',
            DIGITAL: '디지털'
        };

        const subCategoryMap = {
            HARD: '하드',
            JELLY: '젤리',
            CARD: '카드 수납',
            ZFLIP: 'Z플립',
            ROUND: '원형톡',
            HEART: '하트톡',
            ACRYLIC: '아크릴톡',
            AIRPODS_1_2: '에어팟1/2세대',
            AIRPODS_PRO: '에어팟PRO',
            AIRPODS_3: '에어팟3세대',
            BUDS: '버즈',
            APPLE_WATCH: '애플워치'
        };

        const mainCategoryText = mainCategoryMap[mainCategory] || '';
        const subCategoryText = subCategoryMap[subCategory] || '';
        document.getElementById('main-category').innerText = `${mainCategoryText} (${subCategoryText})`;
    }
});
