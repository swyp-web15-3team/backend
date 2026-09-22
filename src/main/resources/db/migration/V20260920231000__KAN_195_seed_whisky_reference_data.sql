-- Category describes whisky type; origin and region are separate dimensions.
INSERT INTO whisky_categories (name)
SELECT seed.name
FROM (VALUES
    ('싱글몰트'), ('블렌디드'), ('블렌디드 몰트'),
    ('싱글그레인'), ('블렌디드 그레인'), ('그레인'),
    ('버번'), ('라이'), ('미분류')
) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1 FROM whisky_categories existing WHERE existing.name = seed.name
);

-- Reference data only: keep existing IDs and whisky assignments.
INSERT INTO whisky_origins (name)
SELECT seed.name
FROM (VALUES
    ('스코틀랜드'),
    ('아일랜드'),
    ('미국'),
    ('캐나다'),
    ('일본'),
    ('대만'),
    ('인도'),
    ('대한민국'),
    ('잉글랜드'),
    ('웨일스'),
    ('호주'),
    ('프랑스'),
    ('독일')
) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1 FROM whisky_origins existing WHERE existing.name = seed.name
);

-- Islands is a service filter region, not a separate statutory Scotch region.
INSERT INTO whisky_regions (origin_id, name)
SELECT origin.id, seed.name
FROM whisky_origins origin
CROSS JOIN (VALUES
    ('하이랜드'),
    ('로우랜드'),
    ('스페이사이드'),
    ('아일라'),
    ('캠벨타운'),
    ('아일랜즈')
) AS seed(name)
WHERE origin.name = '스코틀랜드'
    AND NOT EXISTS (
        SELECT 1 FROM whisky_regions existing
        WHERE existing.origin_id = origin.id AND existing.name = seed.name
    );
