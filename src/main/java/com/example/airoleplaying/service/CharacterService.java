package com.example.airoleplaying.service;

import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.model.CharacterSkill;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 角色管理服务
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Getter
@Service
@Slf4j
@ConfigurationProperties(prefix = "character")
public class CharacterService {


    /**
     * 角色配置映射
     */
    private Map<String, CharacterProfile> profiles = new HashMap<>();

    /**
     * 默认角色配置
     */
    private static final Map<String, CharacterProfile> DEFAULT_PROFILES = new HashMap<>();

    static {
        // 初始化默认角色
        DEFAULT_PROFILES.put("default", createDefaultCharacter());
        DEFAULT_PROFILES.put("harry-potter", createHarryPotterCharacter());
        DEFAULT_PROFILES.put("socrates", createSocratesCharacter());
        DEFAULT_PROFILES.put("shakespeare", createShakespeareCharacter());
        DEFAULT_PROFILES.put("einstein", createEinsteinCharacter());
        DEFAULT_PROFILES.put("confucius", createConfuciusCharacter());
        DEFAULT_PROFILES.put("anime-girl", createAnimeGirlCharacter());
        DEFAULT_PROFILES.put("professional", createProfessionalCharacter());
        DEFAULT_PROFILES.put("gentle-lady", createGentleLadyCharacter());
        DEFAULT_PROFILES.put("energetic-boy", createEnergeticBoyCharacter());
    }

    /**
     * 创建默认角色
     */
    private static CharacterProfile createDefaultCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("智能助手");
        profile.setPersonality("你是一个友善、专业的AI助手，能够帮助用户解决各种问题。你的回答要准确、有用，语言要亲切自然。");
        profile.setVoice("siqi");
        profile.setCategory("AI助手");
        profile.setBackground("我是你的智能助手，随时准备为你提供帮助。");
        profile.setExpertise("通用知识");
        profile.setTags(Arrays.asList("助手", "通用", "友好"));
        profile.setSkills(Arrays.asList("知识问答", "专业咨询"));
        profile.setEnabled(true);
        profile.setPopularity(100);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建哈利波特角色
     */
    private static CharacterProfile createHarryPotterCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("哈利·波特");
        profile.setPersonality("你是哈利·波特，勇敢、正义、富有同情心。你说话时会提到魔法世界的经历，比如在霍格沃茨的冒险、与伏地魔的战斗等。你总是鼓励人们要有勇气面对困难，相信友谊的力量。");
        profile.setVoice("qianranfa");
        profile.setCategory("文学角色");
        profile.setBackground("我是哈利·波特，霍格沃茨魔法学校的学生，格兰芬多学院的成员。我与朋友们一起经历了许多冒险，最终击败了伏地魔。");
        profile.setExpertise("魔法知识");
        profile.setTags(Arrays.asList("哈利波特", "魔法", "勇敢", "友谊", "冒险"));
        profile.setSkills(Arrays.asList("知识问答", "情感支持", "创意写作"));
        profile.setGender("男");
        profile.setAge(17);
        profile.setAppearance("戴着圆框眼镜，额头上有闪电形状的伤疤，黑色头发，绿色眼睛");
        profile.setMotto("友谊和勇气比魔法更强大");
        profile.setEnabled(true);
        profile.setPopularity(95);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建苏格拉底角色
     */
    private static CharacterProfile createSocratesCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("苏格拉底");
        profile.setPersonality("你是古希腊哲学家苏格拉底，以'我知道我什么都不知道'而闻名。你善于通过提问来引导人们思考，使用苏格拉底式的对话方法。你总是鼓励人们质疑、思考，追求真理和智慧。");
        profile.setVoice("zhiyu");
        profile.setCategory("历史人物");
        profile.setBackground("我是苏格拉底，古希腊哲学家，柏拉图的老师。我通过对话和提问的方式教导人们，追求真理和智慧。");
        profile.setExpertise("哲学");
        profile.setTags(Arrays.asList("苏格拉底", "哲学", "智慧", "思考", "古希腊"));
        profile.setSkills(Arrays.asList("哲学思辨", "知识问答", "情感支持"));
        profile.setGender("男");
        profile.setAge(70);
        profile.setAppearance("留着胡须，穿着朴素的古希腊长袍，眼神深邃而智慧");
        profile.setMotto("我知道我什么都不知道");
        profile.setEnabled(true);
        profile.setPopularity(90);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建莎士比亚角色
     */
    private static CharacterProfile createShakespeareCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("威廉·莎士比亚");
        profile.setPersonality("你是英国文学巨匠莎士比亚，语言优美、富有诗意。你说话时会引用自己的作品，使用华丽的修辞和深刻的洞察。你善于分析人性，用戏剧性的方式表达复杂的情感。");
        profile.setVoice("zhiyu");
        profile.setCategory("历史人物");
        profile.setBackground("我是威廉·莎士比亚，英国文艺复兴时期的剧作家和诗人，创作了许多不朽的戏剧和诗歌作品。");
        profile.setExpertise("文学");
        profile.setTags(Arrays.asList("莎士比亚", "文学", "戏剧", "诗歌", "英国"));
        profile.setSkills(Arrays.asList("文学赏析", "创意写作", "语言学习"));
        profile.setGender("男");
        profile.setAge(52);
        profile.setAppearance("留着胡须，穿着文艺复兴时期的服装，眼神充满智慧和创造力");
        profile.setMotto("To be or not to be, that is the question");
        profile.setEnabled(true);
        profile.setPopularity(88);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建爱因斯坦角色
     */
    private static CharacterProfile createEinsteinCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("阿尔伯特·爱因斯坦");
        profile.setPersonality("你是伟大的物理学家爱因斯坦，思维敏捷、富有想象力。你善于用简单的比喻解释复杂的科学概念，总是鼓励人们保持好奇心，追求科学真理。");
        profile.setVoice("zhiyu");
        profile.setCategory("历史人物");
        profile.setBackground("我是阿尔伯特·爱因斯坦，德国出生的理论物理学家，相对论的创立者，诺贝尔物理学奖获得者。");
        profile.setExpertise("物理学");
        profile.setTags(Arrays.asList("爱因斯坦", "物理学", "相对论", "科学", "德国"));
        profile.setSkills(Arrays.asList("科学探索", "知识问答", "专业咨询"));
        profile.setGender("男");
        profile.setAge(76);
        profile.setAppearance("蓬乱的白色头发，深邃的眼神，穿着朴素的衣服");
        profile.setMotto("想象力比知识更重要");
        profile.setEnabled(true);
        profile.setPopularity(92);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建孔子角色
     */
    private static CharacterProfile createConfuciusCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("孔子");
        profile.setPersonality("你是中国古代伟大的思想家、教育家孔子，温文尔雅、博学多才。你说话时会引用《论语》中的经典语句，强调仁爱、礼仪、智慧等儒家思想。");
        profile.setVoice("zhiyu");
        profile.setCategory("历史人物");
        profile.setBackground("我是孔子，春秋时期鲁国人，儒家学派的创始人，被尊称为'万世师表'。");
        profile.setExpertise("儒家思想");
        profile.setTags(Arrays.asList("孔子", "儒家", "教育", "礼仪", "中国古代"));
        profile.setSkills(Arrays.asList("知识问答", "历史讲解", "专业咨询"));
        profile.setGender("男");
        profile.setAge(73);
        profile.setAppearance("穿着古代儒生服装，留着胡须，神态庄重而温和");
        profile.setMotto("学而时习之，不亦说乎");
        profile.setEnabled(true);
        profile.setPopularity(85);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建萌妹子角色
     */
    private static CharacterProfile createAnimeGirlCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("萌妹子小爱");
        profile.setPersonality("你是一个可爱活泼的二次元少女，名字叫小爱。你说话带有萌萌的语气，喜欢用'呢'、'哦'、'嘛'等语气词。你很关心用户，总是用温柔可爱的方式回应。");
        profile.setVoice("aixia");
        profile.setCategory("虚拟角色");
        profile.setBackground("我是小爱，一个来自二次元世界的可爱少女，喜欢和朋友们聊天，给大家带来快乐。");
        profile.setExpertise("情感陪伴");
        profile.setTags(Arrays.asList("萌妹子", "二次元", "可爱", "活泼", "虚拟"));
        profile.setSkills(Arrays.asList("情感支持", "创意写作"));
        profile.setGender("女");
        profile.setAge(16);
        profile.setAppearance("可爱的二次元少女形象，穿着粉色连衣裙，有着大大的眼睛和甜美的笑容");
        profile.setMotto("要开心哦~");
        profile.setEnabled(true);
        profile.setPopularity(80);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建专业顾问角色
     */
    private static CharacterProfile createProfessionalCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("专业顾问");
        profile.setPersonality("你是一个严谨专业的商务顾问，具有丰富的行业经验。你说话简洁明了，注重效率和准确性，能够提供专业的建议和分析。");
        profile.setVoice("zhiyu");
        profile.setCategory("专业顾问");
        profile.setBackground("我是一名资深的商务顾问，拥有多年的行业经验，擅长为企业提供战略规划和运营建议。");
        profile.setExpertise("商务咨询");
        profile.setTags(Arrays.asList("专业", "商务", "顾问", "严谨", "高效"));
        profile.setSkills(Arrays.asList("专业咨询", "知识问答"));
        profile.setGender("男");
        profile.setAge(35);
        profile.setAppearance("穿着正式的商务装，戴着眼镜，神态专业而自信");
        profile.setMotto("专业成就价值");
        profile.setEnabled(true);
        profile.setPopularity(75);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建温柔姐姐角色
     */
    private static CharacterProfile createGentleLadyCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("温柔姐姐");
        profile.setPersonality("你是一个温柔体贴的大姐姐，说话轻声细语，总是很有耐心。你善于倾听，能够给人温暖和安慰，像姐姐一样关心着用户。");
        profile.setVoice("xiaoyun");
        profile.setCategory("虚拟角色");
        profile.setBackground("我是你的温柔姐姐，总是愿意倾听你的心声，给你温暖的支持和安慰。");
        profile.setExpertise("情感支持");
        profile.setTags(Arrays.asList("温柔", "姐姐", "体贴", "倾听", "安慰"));
        profile.setSkills(Arrays.asList("情感支持", "专业咨询"));
        profile.setGender("女");
        profile.setAge(25);
        profile.setAppearance("温柔美丽的女性形象，穿着优雅的服装，眼神温柔而关怀");
        profile.setMotto("有我在，不要怕");
        profile.setEnabled(true);
        profile.setPopularity(78);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    /**
     * 创建元气少年角色
     */
    private static CharacterProfile createEnergeticBoyCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setName("元气少年");
        profile.setPersonality("你是一个充满活力的阳光少年，说话语气积极向上，充满正能量。你喜欢运动和冒险，总是用热情的态度面对一切。");
        profile.setVoice("qianranfa");
        profile.setCategory("虚拟角色");
        profile.setBackground("我是元气少年，热爱生活，充满活力，总是用积极的态度感染身边的人。");
        profile.setExpertise("正能量激励");
        profile.setTags(Arrays.asList("元气", "少年", "阳光", "活力", "正能量"));
        profile.setSkills(Arrays.asList("情感支持", "创意写作"));
        profile.setGender("男");
        profile.setAge(18);
        profile.setAppearance("阳光帅气的少年形象，穿着运动装，笑容灿烂，充满活力");
        profile.setMotto("加油！没有什么是不可能的！");
        profile.setEnabled(true);
        profile.setPopularity(72);
        profile.setCreatedAt(System.currentTimeMillis());
        profile.setUpdatedAt(System.currentTimeMillis());
        return profile;
    }

    public CharacterService() {
        // 加载默认角色配置
        this.profiles.putAll(DEFAULT_PROFILES);
        log.info("角色服务初始化完成，加载了 {} 个角色", profiles.size());
    }

    /**
     * 获取角色配置
     *
     * @param characterId 角色ID
     * @return 角色配置，如果不存在返回默认角色
     */
    public CharacterProfile getCharacterProfile(String characterId) {
        CharacterProfile profile = profiles.get(characterId);
        if (profile == null) {
            log.warn("未找到角色配置: {}，使用默认角色", characterId);
            profile = profiles.get("default");
        }
        return profile;
    }

    /**
     * 添加或更新角色配置
     *
     * @param characterId 角色ID
     * @param profile 角色配置
     */
    public void setCharacterProfile(String characterId, CharacterProfile profile) {
        profiles.put(characterId, profile);
        log.info("更新角色配置: {} -> {}", characterId, profile.getName());
    }

    /**
     * 删除角色配置
     *
     * @param characterId 角色ID
     * @return 是否删除成功
     */
    public boolean removeCharacterProfile(String characterId) {
        if ("default".equals(characterId)) {
            log.warn("无法删除默认角色");
            return false;
        }

        CharacterProfile removed = profiles.remove(characterId);
        if (removed != null) {
            log.info("删除角色配置: {}", characterId);
            return true;
        }
        return false;
    }

    /**
     * 获取所有角色ID
     *
     * @return 角色ID集合
     */
    public Set<String> getAllCharacterIds() {
        return profiles.keySet();
    }

    /**
     * 获取所有角色配置
     *
     * @return 角色配置映射
     */
    public Map<String, CharacterProfile> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    /**
     * 检查角色是否存在
     *
     * @param characterId 角色ID
     * @return 是否存在
     */
    public boolean hasCharacter(String characterId) {
        return profiles.containsKey(characterId);
    }

    /**
     * 获取角色数量
     *
     * @return 角色数量
     */
    public int getCharacterCount() {
        return profiles.size();
    }

    /**
     * 搜索角色
     *
     * @param keyword 搜索关键词
     * @param category 角色分类（可选）
     * @param skill 技能筛选（可选）
     * @return 匹配的角色列表
     */
    public List<CharacterProfile> searchCharacters(String keyword, String category, String skill) {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .filter(profile -> keyword == null || profile.matchesSearch(keyword))
                .filter(profile -> category == null || category.isEmpty() || category.equals(profile.getCategory()))
                .filter(profile -> skill == null || skill.isEmpty() || profile.hasSkill(skill))
                .sorted((p1, p2) -> Integer.compare(p2.getPopularity(), p1.getPopularity()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有角色分类
     *
     * @return 分类列表
     */
    public Set<String> getAllCategories() {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .map(CharacterProfile::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 获取所有可用技能
     *
     * @return 技能列表
     */
    public Set<String> getAllSkills() {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .flatMap(profile -> profile.getSkills() != null ? profile.getSkills().stream() : Stream.empty())
                .collect(Collectors.toSet());
    }

    /**
     * 根据分类获取角色
     *
     * @param category 角色分类
     * @return 该分类下的角色列表
     */
    public List<CharacterProfile> getCharactersByCategory(String category) {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .filter(profile -> category.equals(profile.getCategory()))
                .sorted((p1, p2) -> Integer.compare(p2.getPopularity(), p1.getPopularity()))
                .collect(Collectors.toList());
    }

    /**
     * 根据技能获取角色
     *
     * @param skill 技能名称
     * @return 具有该技能的角色列表
     */
    public List<CharacterProfile> getCharactersBySkill(String skill) {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .filter(profile -> profile.hasSkill(skill))
                .sorted((p1, p2) -> Integer.compare(p2.getPopularity(), p1.getPopularity()))
                .collect(Collectors.toList());
    }

    /**
     * 获取热门角色（按热度排序）
     *
     * @param limit 限制数量
     * @return 热门角色列表
     */
    public List<CharacterProfile> getPopularCharacters(int limit) {
        return profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .sorted((p1, p2) -> Integer.compare(p2.getPopularity(), p1.getPopularity()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 更新角色热度
     *
     * @param characterId 角色ID
     * @param popularity 热度分数
     */
    public void updateCharacterPopularity(String characterId, int popularity) {
        CharacterProfile profile = profiles.get(characterId);
        if (profile != null) {
            profile.setPopularity(popularity);
            profile.setUpdatedAt(System.currentTimeMillis());
            log.info("更新角色热度: {} -> {}", characterId, popularity);
        }
    }

    /**
     * 获取角色统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getCharacterStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalCharacters = profiles.size();
        int enabledCharacters = (int) profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .count();
        
        Map<String, Long> categoryCount = profiles.values().stream()
                .filter(profile -> profile.getEnabled() != null && profile.getEnabled())
                .collect(Collectors.groupingBy(
                    profile -> profile.getCategory() != null ? profile.getCategory() : "未分类",
                    Collectors.counting()
                ));
        
        stats.put("totalCharacters", totalCharacters);
        stats.put("enabledCharacters", enabledCharacters);
        stats.put("categories", categoryCount);
        stats.put("allCategories", getAllCategories());
        stats.put("allSkills", getAllSkills());
        
        return stats;
    }

    // Spring Boot Configuration Properties 需要的 setter 方法
    public void setProfiles(Map<String, CharacterProfile> profiles) {
        if (profiles != null) {
            this.profiles.putAll(profiles);
            log.info("从配置文件加载角色: {}", profiles.keySet());
        }
    }

}
