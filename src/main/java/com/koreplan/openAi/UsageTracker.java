package com.koreplan.openAi;

import java.time.YearMonth;

import org.springframework.stereotype.Component;

@Component
public class UsageTracker {
	private double totalCostThisMonth = 0.0;
    private YearMonth currentMonth = YearMonth.now();
    private final double BUDGET_LIMIT = 5.0; // $5 제한

    // 사용 요금 계산: 입력 + 출력 토큰 기반
    public synchronized boolean canProceed(int inputTokens, int outputTokens) {
        
    	YearMonth now = YearMonth.now();
    	double cost = calculateCost(inputTokens, outputTokens);
        
        if (!now.equals(currentMonth)) {
            // 새로운 달이면 초기화
            currentMonth = now;
            totalCostThisMonth = 0.0;
        }

        if (totalCostThisMonth + cost > BUDGET_LIMIT) {
            return false;
        }

        totalCostThisMonth += cost;
        return true;
    }

    private double calculateCost(int inputTokens, int outputTokens) {
    	// 3.5터보
//        double inputCost = inputTokens / 1000.0 * 0.0005;
//        double outputCost = outputTokens / 1000.0 * 0.0015;
//    	4o
        double inputCost = inputTokens / 1000.0 * 0.005;
        double outputCost = outputTokens / 1000.0 * 0.015;
        return inputCost + outputCost;
    }

    public double getTotalCostThisMonth() {
        return totalCostThisMonth;
    }
}
