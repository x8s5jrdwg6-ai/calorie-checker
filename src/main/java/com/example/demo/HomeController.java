package com.example.demo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//import com.example.demo.HomeController.IntakeEditRow;

@Controller
public class HomeController {

	private final JdbcTemplate jdbc;
	private final IntakeService intakeSvc;

//	// ログイン未実装なので固定（DBのregist_user_idと合わせる）
//	private static final String USER_ID = "test";
	
	private static final String UID_COOKIE = "cc_uid";

	private String resolveUserId(HttpServletRequest req, HttpServletResponse res) {
	    // Cookieがあればそれを使う
	    if (req.getCookies() != null) {
	        for (Cookie c : req.getCookies()) {
	            if (UID_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
	            	String sql = """
	            				SELECT COUNT(*)
	            				FROM users
	            				WHERE user_id = ?
	            			""";
	            	if(jdbc.queryForObject(sql, int.class, c.getValue()) == 1) {
	            		return c.getValue();
	            	}
	            }
	        }
	    }

	    // 無ければ新規UUID
	    String userId = UUID.randomUUID().toString();
	    // usersに登録（既にあってもOK）
	    jdbc.update("""
	    		INSERT INTO users(user_id, user_name, password, regist_date)
	    		VALUES (?, ?, ?, CURRENT_DATE)
	    		ON CONFLICT (user_id) DO NOTHING;
	    """, userId, "test", "test");

	    // Cookie保存
	    Cookie cookie = new Cookie(UID_COOKIE, userId);
	    cookie.setPath("/");
	    cookie.setHttpOnly(true);
	    cookie.setMaxAge(60 * 60 * 24 * 365); // 1年

	    // Renderはリバプロなのでヘッダを見る
	    String xfProto = req.getHeader("X-Forwarded-Proto");
	    boolean https = "https".equalsIgnoreCase(xfProto) || req.isSecure();
	    cookie.setSecure(https);

	    // SameSite=Lax（Servlet APIの都合で setAttribute 方式）
	    cookie.setAttribute("SameSite", "Lax");

	    res.addCookie(cookie);
	    return userId;
	}
	

	public HomeController(JdbcTemplate jdbc, IntakeRepository intakeRepo, IntakeService intakeSvc) {
		this.jdbc = jdbc;
		this.intakeSvc = intakeSvc;
	}

	/*--------------------------------------
		record
	--------------------------------------*/
	public record IntakeRow(long intakeId, String eatenDate, String eatenTime, double qty, String foodName, String flavorName, int calorie) {
		public int kcalTotal() {
			return (int) Math.round(calorie * qty);
		}
	}
	
	/*--------------------------------------
		画面遷移
	--------------------------------------*/
	// 初期表示想定
	// 日付選択時
	// 「TOPへ戻る」押下時
	@GetMapping("/")
	public String home(@RequestParam(name = "date", required = false) String date, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
//		System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
//		System.out.println(userId);
//		System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		// 三項演算子（条件 ? 真のときの値 : 偽のときの値）
		LocalDate targetDate = (date == null || date.isBlank()) ? LocalDate.now(): LocalDate.parse(date);
		// modelに格納
		model.addAttribute("targetDate", targetDate.toString());
		// 合計カロリー取得
		model.addAttribute("totalKcal", intakeSvc.calcTotalCal(userId, targetDate));
		// getDailyRecordsで対象日の履歴を取得
		model.addAttribute("targetDateAteRecords", intakeSvc.getDailyRecords(userId, targetDate));
		model.addAttribute("prevDate", targetDate.minusDays(1).toString());
		model.addAttribute("todayDate", LocalDate.now().toString());
		model.addAttribute("nextDate", targetDate.plusDays(1).toString());

		// このreturnは画面を指定している（.../templates/home.html）
		return "home";
	}
	
	// TOP画面：詳細押下時
	@GetMapping("/intake/detail")
	public String intakeDetail(@RequestParam("intakeId") long intakeId, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// IDから詳細情報を取得
		Map<String, Object> map = intakeSvc.getIntakeDetail(userId, intakeId);
		if(map == null) {
			model.addAttribute("errorMsg", "該当の履歴が見つかりません");
			return "home";
		}
		
		model.addAttribute("intakeId", map.get("intakeId"));
		model.addAttribute("eatenDatetime", (map.get("eatenDate") + " " + map.get("eatenTime")));
		model.addAttribute("makerName", map.get("makerName"));
		model.addAttribute("foodName", map.get("foodName"));
		model.addAttribute("flavorName", map.get("flavorName"));
		model.addAttribute("calorie", map.get("calorie"));
		model.addAttribute("protein", map.get("protein"));
		model.addAttribute("lipid", map.get("lipid"));
		model.addAttribute("carbo", map.get("carbo"));
		model.addAttribute("salt", map.get("salt"));
		
		return "intake_detail";
	}
	
	// 詳細画面：編集押下時
	@GetMapping("/intake/edit")
	public String intakeEdit(@RequestParam("intakeId") long intakeId, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// IDから詳細情報を取得
		Map<String, Object> map = intakeSvc.getIntakeDetail(userId, intakeId);
		if(map == null) {
			model.addAttribute("errorMsg", "該当の履歴が見つかりません");
			return "home";
		}
				
		model.addAttribute("intakeId", map.get("intakeId"));
		model.addAttribute("eatenDate", (map.get("eatenDate")));
		model.addAttribute("eatenTime", (map.get("eatenTime")));
		model.addAttribute("foodName", map.get("foodName"));
		model.addAttribute("flavorName", map.get("flavorName"));
		model.addAttribute("calorie", map.get("calorie"));

		return "intake_edit";
	}
	
	// ナビゲーション：食べた押下時
	// 食べた登録画面：食品選択画面：「←メーカー選択へ戻る」押下時
	@GetMapping("/eat")
	public String eatMaker(Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDに紐づくメーカー一覧を取得
		List<Map<String, Object>> makers = intakeSvc.getMakerList(userId);
		
		model.addAttribute("makers", makers);
		return "eat_maker";
	}
	
	// 食べた登録画面：メーカ選択画面：メーカー選択時
	@GetMapping("/eat/foods")
	public String eatFoods(@RequestParam("makerId") long makerId, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 表示するメーカー名を取得
		String makerName = intakeSvc.getMakerName(userId, makerId);

		// ユーザーIDと選択したメーカーに紐づく食品一覧を取得
		List<Map<String, Object>> foods = intakeSvc.getFoodList(userId, makerId);
		
		model.addAttribute("makerId", makerId);
		model.addAttribute("makerName", makerName);
		model.addAttribute("foods", foods);
		return "eat_food";
	}
	
	// 食べた登録画面：食品選択画面：食品選択時
	@GetMapping("/eat/flavors")
	public String eatFlavors(@RequestParam("foodId") long foodId,
			@RequestParam(name="error", required=false) String error,
			Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得
		Map<String, Object> headerInfo = intakeSvc.getHeaderInfo(userId, foodId);

		// ユーザーIDと食品IDに紐づく味一覧を取得
		List<Map<String, Object>> flavorList = intakeSvc.getFlavorList(userId, foodId);
		
		model.addAttribute("header", headerInfo);
		model.addAttribute("flavors", flavorList);
		model.addAttribute("error", error);
		return "eat_flavor";
	}
	
	// ナビゲーション：メーカー登録押下時
	@GetMapping("/makers/new")
	public String makerNew() {
		return "maker_new";
	}
	
	
	// ナビゲーション：食品情報登録押下時
	@GetMapping("/foods/new")
	public String foodNew(Model model,
			@RequestParam(name="makerId", required=false) Long makerId,
			@RequestParam(name="error", required=false) String error,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// メーカー一覧を取得
		List<Map<String, Object>> makers = jdbc.queryForList(
				"SELECT food_maker_id, maker_name FROM food_maker WHERE regist_user_id=? ORDER BY maker_name",
				userId
				);

		model.addAttribute("makers", makers);
		model.addAttribute("selectedMakerId", makerId); // 初期選択用
		model.addAttribute("error", error);
		return "food_new";
	}
	
	// ナビゲーション：栄養情報登録押下時
	// 食品情報登録画面：「食品を選択して栄養情報登録へ進む」押下時
	@GetMapping("/flavors/new")
	public String flavorNew(Model model,
			@RequestParam(name="foodId", required=false) Long foodId,
			@RequestParam(name="error", required=false) String error,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 食品一覧を取得
		List<Map<String, Object>> foods = jdbc.queryForList(
				"SELECT food_id, food_name FROM food WHERE regist_user_id=? ORDER BY food_name",
				userId
				);

		model.addAttribute("foods", foods);
		model.addAttribute("selectedFoodId", foodId); // 初期選択用
		model.addAttribute("error", error);
		return "flavor_new";
	}
	
	/*--------------------------------------
		詳細画面
	--------------------------------------*/
	// 削除押下時
	@PostMapping("/intake/delete")
	public String delete(@RequestParam("intakeId") long intakeId,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.delIntake(userId, intakeId);
		return "redirect:/";
	}


	/*--------------------------------------
		メーカー登録画面
	--------------------------------------*/
	// メーカー登録画面で登録押下時
	@PostMapping("/makers/create")
	public String makerCreate(@RequestParam("makerName") String makerName, RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// メーカー重複チェック
		if(intakeSvc.chkDepliMaker(userId, makerName) == -1) {
			ra.addFlashAttribute("msg", "同じメーカーが既に登録されています");
			return "redirect:/makers/new";
		}

		intakeSvc.insFoodMaker(userId, makerName);
		ra.addFlashAttribute("msg", "メーカーを登録しました");
		return "redirect:/makers/new";
	}


	/*--------------------------------------
		食品登録画面
	--------------------------------------*/
	// 食品登録時
	@PostMapping("/foods/create-and-next")
	public String foodCreateAndNext(@RequestParam("makerId") long makerId,
			@RequestParam("foodName") String foodName,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 重複チェック
		if(intakeSvc.chkDepliFood(userId, foodName, makerId) == -1) {
			ra.addFlashAttribute("msg", "同じメーカー内に同名の食品が既に登録されています");
			return "redirect:/foods/new";
		}
		// 食品情報登録
		long foodId = intakeSvc.insFood(userId, foodName, makerId);

		ra.addFlashAttribute("msg", "食品を登録しました。続けて栄養情報を登録してください");
		return "redirect:/flavors/new?foodId=" + Long.toString(foodId);
	}

	/*--------------------------------------
		栄養情報登録画面
	--------------------------------------*/
	@PostMapping("/flavors/create")
	public String flavorCreate(
			@RequestParam("foodId") long foodId,
			@RequestParam("flavorName") String flavorName,
			@RequestParam("calorie") int calorie,
			@RequestParam(name="protein", required=false) Double protein,
			@RequestParam(name="lipid", required=false) Double lipid,
			@RequestParam(name="carbo", required=false) Double carbo,
			@RequestParam(name="salt", required=false) Double salt,
			@RequestParam(name="plainFlg", required=false) String plainFlg,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res
			) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// プレーンにチェックがあれば味を「プレーン」に固定
		if ("1".equals(plainFlg)) {
			flavorName = "プレーン";
		}

		// 重複チェック
		if(intakeSvc.chkDepliFlavor(userId, flavorName, foodId) == -1) {
			ra.addFlashAttribute("msg", "同じ味の栄養情報が既に登録されています");
			return "redirect:/flavors/new";
		}

		// 栄養情報登録
		intakeSvc.insFlavor(userId, flavorName, foodId, calorie, protein, lipid, carbo, salt);

		ra.addFlashAttribute("msg", "栄養情報を登録しました");
		return "redirect:/flavors/new";
	}

	/*--------------------------------------
		食べた登録画面
	--------------------------------------*/
	// 食べた！押下時
	@PostMapping("/eat/record")
	public String eatRecord(@RequestParam("flavorId") long flavorId,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.insIntake(userId, flavorId);
		ra.addFlashAttribute("msg", "食べた！を記録しました。");
		return "redirect:/";
	}

	@PostMapping("/intake/update")
	public String intakeUpdate(
			@RequestParam("intakeId") long intakeId,
			@RequestParam("eatenDate") LocalDate eatenDate,
			@RequestParam("eatenTime") LocalTime eatenTime,
            HttpServletRequest req,
            HttpServletResponse res
			){
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.updIntake(userId, intakeId, eatenDate, eatenTime);

		return "redirect:/intake/detail?intakeId=" + intakeId;
	}

}
