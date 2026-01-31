package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.example.demo.HomeController.IntakeRow;

@Service
public class IntakeService {

	private final IntakeRepository intakeRepo;

	public IntakeService(IntakeRepository intakeRepo) {
		this.intakeRepo = intakeRepo;
	}
	
	public List<IntakeRow> getDailyRecords(String userId, LocalDate date) {
		return intakeRepo.getDailyRecords(userId, date);
	}
	
	public int calcTotalCal(String userId, LocalDate eatenDate) {
		return intakeRepo.calcTotalCal(userId, eatenDate);
	}

	public int insIntake(String userId, long flavorId) {
		LocalDateTime now = LocalDateTime.now();
		return intakeRepo.insIntake(userId, flavorId, now.toLocalDate(), now.toLocalTime());
	}
	
	public void updIntake(String userId, long intakeId, LocalDate eatenDate, LocalTime eatenTime) {
		intakeRepo.updIntake(userId, intakeId, eatenDate, eatenTime);
	}
	
	public int delIntake(String userId, long intakeId) {
		return intakeRepo.delIntake(intakeId, userId);
	}
	
	public int insFoodMaker(String userId, String makerName) {
		return intakeRepo.insFoodMaker(userId, makerName);
	}
	
	public int chkDepliFood(String userId, String foodName, long makerId) {
		//重複チェック
		if(intakeRepo.chkDepliFood(userId, foodName, makerId) != 0) {
			return -1;	// エラー
		}
		return 0;
	}
	
	public int insFood(String userId, String foodName, long makerId) {
		return intakeRepo.insFood(userId, foodName, makerId);
	}
	
	public long getRegistFoodId(String userId, String foodName, long makerId) {
		return intakeRepo.getRegistFoodId(userId, foodName, makerId);
	}
	
	public int chkDepliMaker(String userId, String makerName) {
		if(intakeRepo.chkDepliMaker(userId, makerName) != 0) {
			return -1;	// エラー
		}
		return 0;
	}
	
	public int chkDepliFlavor(String userId, String flavorName, long foodId) {
		//重複チェック
		if(intakeRepo.chkDepliFlavor(userId, flavorName, foodId) != 0) {
			return -1;	// エラー
		}
		return 0;
	}
	
	public void insFlavor(String userId, String flavorName, long foodId, int calorie, Double protein, Double lipid, Double carbo, Double salt) {
		intakeRepo.insFlavor(userId, flavorName, foodId, calorie, protein, lipid, carbo, salt);
		
	}
	
	public Map<String, Object> getIntakeDetail(String userId, long intakeId) {
		return intakeRepo.getIntakeDetail(userId, intakeId);
	}
	
	public List<Map<String, Object>> getMakerList(String userId){
		return intakeRepo.getMakerList(userId);
	}
	
	public String getMakerName(String userId, long makerId) {
		return intakeRepo.getMakerName(userId, makerId);
	}
	
	public List<Map<String, Object>> getFoodList(String userId, long makerId){
		return intakeRepo.getFoodList(userId, makerId);
	}
	
	public Map<String, Object> getHeaderInfo (String userId, long foodId) {
		return intakeRepo.getHeaderInfo(userId, foodId);
	}
	
	public List<Map<String, Object>> getFlavorList(String userId, long foodId){
		return intakeRepo.getFlavorList(userId, foodId);
	}
	
	public String chkUserId(HttpServletRequest req, String uidCookie) {
		String userId = "";
		if (req.getCookies() != null) {
	        for (Cookie c : req.getCookies()) {
	            if (uidCookie.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
	            	userId = intakeRepo.chkUserId(c);
	            }
	            if(!userId.isEmpty()) {
	            	break;
	            }
	        }
	    }
		return userId;
	}
	
	public String registUserId() {
		String userId = UUID.randomUUID().toString();
		intakeRepo.registUserId(userId);
		return userId;
	}
	
	public Cookie setCookie(String uidCookie, String userId, HttpServletRequest req) {
		// Cookie保存
	    Cookie cookie = new Cookie(uidCookie, userId);
	    cookie.setPath("/");
	    cookie.setHttpOnly(true);
	    cookie.setMaxAge(60 * 60 * 24 * 365); // 1年

	    // Renderはリバプロなのでヘッダを見る
	    String xfProto = req.getHeader("X-Forwarded-Proto");
	    boolean https = "https".equalsIgnoreCase(xfProto) || req.isSecure();
	    cookie.setSecure(https);

	    // SameSite=Lax（Servlet APIの都合で setAttribute 方式）
	    cookie.setAttribute("SameSite", "Lax");
	    
	    return cookie;
	}
	
	public void updLastAccessDate(String userId) {
		intakeRepo.updLastAccessDate(userId);
	}
}
