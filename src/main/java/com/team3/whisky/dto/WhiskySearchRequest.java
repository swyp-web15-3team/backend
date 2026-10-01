package com.team3.whisky.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record WhiskySearchRequest(
    @Parameter(description = "위스키 이름 검색어입니다. 공백 제거 후 1~255자여야 합니다.") String query,
    @Parameter(name = "categoryId", in = ParameterIn.QUERY, description = "여러 카테고리는 `?categoryId=1&categoryId=2`처럼 반복해서 전달합니다. 쉼표로 연결하지 않습니다.", style = ParameterStyle.FORM, explode = Explode.TRUE, array = @ArraySchema(schema = @Schema(type = "integer", format = "int64", example = "1"))) List<Long> categoryId,
    @Parameter(description = "원산지 ID로 필터링합니다.") Long originId,
    @Parameter(description = "지역 ID로 필터링합니다. originId도 전달하면 해당 원산지에 속해야 합니다.") Long regionId,
    @Parameter(description = "용량(ml)으로 필터링합니다.") Integer volumeMl,
    @Parameter(description = "판매 국가로 필터링합니다.", schema = @Schema(allowableValues = {
            "KR", "JP"})) String countryCode,
    @Parameter(description = "면세점 판매 여부로 필터링합니다.") Boolean isDutyFree,
    @Parameter(description = "최저가 기준의 원화 최소 가격입니다(포함). 한국·일본 가격 중 낮은 값을 비교하며 minPrice ≤ maxPrice여야 합니다.", example = "100000", schema = @Schema(minimum = "0")) BigDecimal minPrice,
    @Parameter(description = "최저가 기준의 원화 최대 가격입니다(포함). 한국·일본 가격 중 낮은 값을 비교하며 minPrice ≤ maxPrice여야 합니다.", example = "300000", schema = @Schema(minimum = "0")) BigDecimal maxPrice,
    @Parameter(description = "일본 할인율(%)의 최소값(포함)입니다. 80% 이상은 이 값만 80으로 입력합니다. 두 값을 함께 보내면 minPriceDiffPercent < maxPriceDiffPercent여야 합니다.", example = "20", schema = @Schema(minimum = "0", exclusiveMinimum = false, maximum = "100", exclusiveMaximum = true)) BigDecimal minPriceDiffPercent,
    @Parameter(description = "일본 할인율(%)의 최대값(미포함)입니다. 20% 미만은 이 값만 20으로 입력합니다. 두 값을 함께 보내면 minPriceDiffPercent < maxPriceDiffPercent여야 합니다.", example = "40", schema = @Schema(minimum = "0", exclusiveMinimum = true, maximum = "100", exclusiveMaximum = false)) BigDecimal maxPriceDiffPercent,
    @Parameter(description = "정렬 기준입니다.", schema = @Schema(allowableValues = {"name,asc", "name,desc", "id,asc",
            "id,desc"}, defaultValue = "name,asc")) String sort,
    @Parameter(description = "0부터 시작하는 페이지 번호입니다.", schema = @Schema(minimum = "0", defaultValue = "0")) Integer page,
    @Parameter(description = "페이지 크기입니다(1~50).", schema = @Schema(minimum = "1", maximum = "50", defaultValue = "20")) Integer size){
}
