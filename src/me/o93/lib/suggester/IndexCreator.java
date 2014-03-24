package me.o93.lib.suggester;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;

/**
 * テキスト
 * @author okuda
 *
 */
public final class IndexCreator {
    private static final String TAG = "IndexCreator";
    private static final String REG_KANJI ="\\p{InCJKUnifiedIdeographs}+";
    private static final String REG_WORD =
            REG_KANJI + "(\\p{InHiragana}|\\p{InKatakana})+" + REG_KANJI;
    private static final String REG_KANA = "(\\p{InHiragana}|\\p{InKatakana})+";
    private static final String REG_ENGLISH = "\\p{Lower}+";
    private static final String REG_HALF = "[ -~｡-ﾟ]+";
    private static IndexCreator sInstance;

    /**
     * インスタンスを取得.
     * @return インスタンス
     */
    public static IndexCreator getInstance() {
        if (sInstance == null) sInstance = new IndexCreator();
        return sInstance;
    }

    private final HashMap<Character, CharSequence> k2RMap;
    private final HashMap<CharSequence, CharSequence> r2KMap;
    private final HashMap<CharSequence, CharSequence> lastMap;

    private StringBuilder mTempBuilder1;
    private StringBuilder mTempBuilder2;
    private StringBuilder mEnglishBuilder;

    private IndexCreator() {
        k2RMap = new HashMap<Character, CharSequence>();
        // ひらがな -> ローマ字
        k2RMap.put('あ', "a");  k2RMap.put('い', "i");  k2RMap.put('う', "u");  k2RMap.put('え', "e");  k2RMap.put('お', "o");
        k2RMap.put('か', "ka"); k2RMap.put('き', "ki"); k2RMap.put('く', "ku"); k2RMap.put('け', "ke"); k2RMap.put('こ', "ko");
        k2RMap.put('さ', "sa"); k2RMap.put('し', "si"); k2RMap.put('す', "su"); k2RMap.put('せ', "se"); k2RMap.put('そ', "so");
        k2RMap.put('た', "ta"); k2RMap.put('ち', "ti"); k2RMap.put('つ', "tu"); k2RMap.put('て', "te"); k2RMap.put('と', "to");
        k2RMap.put('な', "na"); k2RMap.put('に', "ni"); k2RMap.put('ぬ', "nu"); k2RMap.put('ね', "ne"); k2RMap.put('の', "no");
        k2RMap.put('は', "ha"); k2RMap.put('ひ', "hi"); k2RMap.put('ふ', "hu"); k2RMap.put('へ', "he"); k2RMap.put('ほ', "ho");
        k2RMap.put('ま', "ma"); k2RMap.put('み', "mi"); k2RMap.put('む', "mu"); k2RMap.put('め', "me"); k2RMap.put('も', "mo");
        k2RMap.put('や', "ya"); k2RMap.put('ゆ', "yu"); k2RMap.put('よ', "yo");
        k2RMap.put('ら', "ra"); k2RMap.put('り', "ri"); k2RMap.put('る', "ru"); k2RMap.put('れ', "re"); k2RMap.put('ろ', "ro");
        k2RMap.put('わ', "wa"); k2RMap.put('を', "o");  k2RMap.put('ん', "nn");

        k2RMap.put('が', "ga"); k2RMap.put('ぎ', "gi"); k2RMap.put('ぐ', "gu"); k2RMap.put('げ', "ge"); k2RMap.put('ご', "go");
        k2RMap.put('ざ', "za"); k2RMap.put('じ', "zi"); k2RMap.put('ず', "zu"); k2RMap.put('ぜ', "ze"); k2RMap.put('ぞ', "zo");
        k2RMap.put('だ', "da"); k2RMap.put('ぢ', "zi"); k2RMap.put('づ', "zu"); k2RMap.put('で', "de"); k2RMap.put('ど', "do");
        k2RMap.put('ば', "ba"); k2RMap.put('び', "bi"); k2RMap.put('ぶ', "bu"); k2RMap.put('べ', "be"); k2RMap.put('ぼ', "bo");
        k2RMap.put('ぱ', "pa"); k2RMap.put('ぴ', "pi"); k2RMap.put('ぷ', "pu"); k2RMap.put('ぺ', "pe"); k2RMap.put('ぽ', "po");
        k2RMap.put('ぁ', "a"); k2RMap.put('ぃ', "i"); k2RMap.put('ぅ', "u"); k2RMap.put('ぇ', "e"); k2RMap.put('ぉ', "o");

        k2RMap.put('ゃ', "ya"); k2RMap.put('ゅ', "yu"); k2RMap.put('ょ', "yo");
        k2RMap.put('っ', "tu"); k2RMap.put('ゎ', "wa"); k2RMap.put('ゔ', "vu");
        k2RMap.put('ゐ', "wyi");

        k2RMap.put('ー', "-"); k2RMap.put('〜', "-"); k2RMap.put('~', "-");
        k2RMap.put('、', ","); k2RMap.put('。', ".");
        k2RMap.put('「', "["); k2RMap.put('」', "]");

        // ローマ字 -> ひらがな
        r2KMap = new HashMap<CharSequence, CharSequence>();
        r2KMap.put("a", "あ"); r2KMap.put("i", "い"); r2KMap.put("u", "う"); r2KMap.put("e", "え"); r2KMap.put("o", "お"); r2KMap.put("n", "ん");

        r2KMap.put("qq", "つ"); r2KMap.put("vv", "つ"); r2KMap.put("ll", "つ"); r2KMap.put("xx", "つ"); r2KMap.put("kk", "つ");
        r2KMap.put("gg", "つ"); r2KMap.put("ss", "つ"); r2KMap.put("zz", "つ"); r2KMap.put("jj", "つ"); r2KMap.put("tt", "つ");
        r2KMap.put("dd", "つ"); r2KMap.put("hh", "つ"); r2KMap.put("ff", "つ"); r2KMap.put("bb", "つ"); r2KMap.put("pp", "つ");
        r2KMap.put("mm", "つ"); r2KMap.put("yy", "つ"); r2KMap.put("rr", "つ"); r2KMap.put("ww", "つ"); r2KMap.put("cc", "つ");

        r2KMap.put("ka", "か"); r2KMap.put("ki", "き"); r2KMap.put("ku", "く"); r2KMap.put("ke", "け"); r2KMap.put("ko", "こ");
        r2KMap.put("ga", "が"); r2KMap.put("gi", "ぎ"); r2KMap.put("gu", "ぐ"); r2KMap.put("ge", "げ"); r2KMap.put("go", "ご");
        r2KMap.put("sa", "さ"); r2KMap.put("si", "し"); r2KMap.put("su", "す"); r2KMap.put("se", "せ"); r2KMap.put("so", "そ");
        r2KMap.put("ca", "か"); r2KMap.put("ci", "し"); r2KMap.put("cu", "く"); r2KMap.put("ce", "せ"); r2KMap.put("co", "こ");
        r2KMap.put("za", "ざ"); r2KMap.put("zi", "じ"); r2KMap.put("zu", "ず"); r2KMap.put("ze", "ぜ"); r2KMap.put("zo", "ぞ");
        r2KMap.put("ta", "た"); r2KMap.put("ti", "ち"); r2KMap.put("tu", "つ"); r2KMap.put("te", "て"); r2KMap.put("to", "と");
        r2KMap.put("da", "だ"); r2KMap.put("di", "ぢ"); r2KMap.put("du", "づ"); r2KMap.put("de", "で"); r2KMap.put("do", "ど");
        r2KMap.put("na", "な"); r2KMap.put("ni", "に"); r2KMap.put("nu", "ぬ"); r2KMap.put("ne", "ね"); r2KMap.put("no", "の");
        r2KMap.put("ha", "は"); r2KMap.put("hi", "ひ"); r2KMap.put("hu", "ふ"); r2KMap.put("fu", "ふ"); r2KMap.put("he", "へ"); r2KMap.put("ho", "ほ");
        r2KMap.put("ba", "ば"); r2KMap.put("bi", "び"); r2KMap.put("bu", "ぶ"); r2KMap.put("be", "べ"); r2KMap.put("bo", "ぼ");
        r2KMap.put("pa", "ぱ"); r2KMap.put("pi", "ぴ"); r2KMap.put("pu", "ぷ"); r2KMap.put("pe", "ぺ"); r2KMap.put("po", "ぽ");
        r2KMap.put("ma", "ま"); r2KMap.put("mi", "み"); r2KMap.put("mu", "む"); r2KMap.put("me", "め"); r2KMap.put("mo", "も");
        r2KMap.put("ya", "や"); r2KMap.put("yu", "ゆ"); r2KMap.put("yo", "よ");
        r2KMap.put("ra", "ら"); r2KMap.put("ri", "り"); r2KMap.put("ru", "る"); r2KMap.put("re", "れ"); r2KMap.put("ro", "ろ");

        r2KMap.put("va", "ゔあ"); r2KMap.put("vi", "ゔい"); r2KMap.put("vu", "ゔ"); r2KMap.put("ve", "ゔえ"); r2KMap.put("vo", "ゔお");
        r2KMap.put("fa", "ふあ"); r2KMap.put("fi", "ふい"); r2KMap.put("fu", "ふ"); r2KMap.put("fe", "ふえ"); r2KMap.put("fo", "ふお");
        r2KMap.put("qa", "くあ"); r2KMap.put("qi", "くい"); r2KMap.put("qu", "く"); r2KMap.put("qe", "くえ"); r2KMap.put("qo", "くお");
        r2KMap.put("ja", "じや"); r2KMap.put("ji", "じ"); r2KMap.put("ju", "じゆ"); r2KMap.put("je", "じえ"); r2KMap.put("jo", "じよ");
        r2KMap.put("wa", "わ"); r2KMap.put("ye", "いえ"); r2KMap.put("wi", "うい"); r2KMap.put("we", "うえ"); r2KMap.put("wo", "を");

        r2KMap.put("wu", "う"); r2KMap.put("n'", "ん"); r2KMap.put("nn", "ん"); r2KMap.put("xn", "ん");

        r2KMap.put("xa", "あ"); r2KMap.put("xi", "い"); r2KMap.put("xu", "う"); r2KMap.put("xe", "え"); r2KMap.put("xo", "お");
        r2KMap.put("la", "あ"); r2KMap.put("li", "い"); r2KMap.put("lu", "う"); r2KMap.put("le", "え"); r2KMap.put("lo", "お");

        r2KMap.put("vya", "ゔや"); r2KMap.put("vyi", "ゔい"); r2KMap.put("vyu", "ゔゆ"); r2KMap.put("vye", "ゔえ"); r2KMap.put("vyo", "ゔよ");
        r2KMap.put("kya", "きや"); r2KMap.put("kyi", "きい"); r2KMap.put("kyu", "きゆ"); r2KMap.put("kye", "きえ"); r2KMap.put("kyo", "きよ");
        r2KMap.put("gya", "ぎや"); r2KMap.put("gyi", "ぎい"); r2KMap.put("gyu", "ぎゆ"); r2KMap.put("gye", "ぎえ"); r2KMap.put("gyo", "ぎよ");
        r2KMap.put("sya", "しや"); r2KMap.put("syi", "しい"); r2KMap.put("syu", "しゆ"); r2KMap.put("sye", "しえ"); r2KMap.put("syo", "しよ");
        r2KMap.put("sha", "しや"); r2KMap.put("shi", "し"); r2KMap.put("shu", "しゆ"); r2KMap.put("she", "しえ"); r2KMap.put("sho", "しよ");
        r2KMap.put("zya", "じや"); r2KMap.put("zyi", "じい"); r2KMap.put("zyu", "じゆ"); r2KMap.put("zye", "じえ"); r2KMap.put("zyo", "じよ");
        r2KMap.put("tya", "ちや"); r2KMap.put("tyi", "ちい"); r2KMap.put("tyu", "ちゆ"); r2KMap.put("tye", "ちえ"); r2KMap.put("tyo", "ちよ");
        r2KMap.put("cha", "ちや"); r2KMap.put("chi", "ち"); r2KMap.put("chu", "ちゆ"); r2KMap.put("che", "ちえ"); r2KMap.put("cho", "ちよ");
        r2KMap.put("cya", "ちや"); r2KMap.put("cyi", "ちい"); r2KMap.put("cyu", "ちゆ"); r2KMap.put("cye", "ちえ"); r2KMap.put("cyo", "ちよ");
        r2KMap.put("dya", "ぢや"); r2KMap.put("dyi", "ぢい"); r2KMap.put("dyu", "ぢゆ"); r2KMap.put("dye", "ぢえ"); r2KMap.put("dyo", "ぢよ");
        r2KMap.put("tsa", "つあ"); r2KMap.put("tsi", "つい"); r2KMap.put("tse", "つえ"); r2KMap.put("tso", "つお");

        r2KMap.put("tha", "てや"); r2KMap.put("thi", "てい"); r2KMap.put("t'i", "てい"); r2KMap.put("thu", "てゆ"); r2KMap.put("the", "てえ"); r2KMap.put("tho", "てよ");
        r2KMap.put("dha", "でや"); r2KMap.put("dhi", "でい"); r2KMap.put("d'i", "でい"); r2KMap.put("dhu", "でゆ"); r2KMap.put("dhe", "でえ"); r2KMap.put("dho", "でよ");
        r2KMap.put("twa", "とあ"); r2KMap.put("twi", "とい"); r2KMap.put("twu", "とう"); r2KMap.put("twe", "とえ"); r2KMap.put("two", "とお"); r2KMap.put("t'u", "とう");
        r2KMap.put("dwa", "どあ"); r2KMap.put("dwi", "どい"); r2KMap.put("dwu", "どう"); r2KMap.put("dwe", "どえ"); r2KMap.put("dwo", "どお"); r2KMap.put("d'u", "どう");

        r2KMap.put("nya", "にや"); r2KMap.put("nyi", "にい"); r2KMap.put("nyu", "にゆ"); r2KMap.put("nye", "にえ"); r2KMap.put("nyo", "によ");
        r2KMap.put("hya", "ひや"); r2KMap.put("hyi", "ひい"); r2KMap.put("hyu", "ひゆ"); r2KMap.put("hye", "ひえ"); r2KMap.put("hyo", "ひよ");
        r2KMap.put("bya", "びや"); r2KMap.put("byi", "びい"); r2KMap.put("byu", "びゆ"); r2KMap.put("bye", "びえ"); r2KMap.put("byo", "びよ");
        r2KMap.put("pya", "ぴや"); r2KMap.put("pyi", "ぴい"); r2KMap.put("pyu", "ぴゆ"); r2KMap.put("pye", "ぴえ"); r2KMap.put("pyo", "ぴよ");

        r2KMap.put("fya", "ふや"); r2KMap.put("fyu", "ふゆ"); r2KMap.put("fyo", "ふよ");
        r2KMap.put("hwa", "ふあ"); r2KMap.put("hwi", "ふい"); r2KMap.put("hwe", "ふえ"); r2KMap.put("hwo", "ふお");

        r2KMap.put("mya", "みや"); r2KMap.put("myi", "みい"); r2KMap.put("myu", "みゆ"); r2KMap.put("mye", "みえ"); r2KMap.put("myo", "みよ");
        r2KMap.put("rya", "りや"); r2KMap.put("ryi", "りい"); r2KMap.put("ryu", "りゆ"); r2KMap.put("rye", "りえ"); r2KMap.put("ryo", "りよ");
        r2KMap.put("kwa", "くあ"); r2KMap.put("kwi", "くい"); r2KMap.put("kwu", "くう"); r2KMap.put("kwe", "くえ"); r2KMap.put("kwo", "くお");
        r2KMap.put("gwa", "ぐあ"); r2KMap.put("gwi", "ぐい"); r2KMap.put("gwu", "ぐう"); r2KMap.put("gwe", "ぐえ"); r2KMap.put("gwo", "ぐお");
        r2KMap.put("jya", "じや"); r2KMap.put("jyi", "じい"); r2KMap.put("jyu", "じゆ"); r2KMap.put("jye", "じえ"); r2KMap.put("jyo", "じよ");

        r2KMap.put("lyi", "い"); r2KMap.put("xyi", "い"); r2KMap.put("lye", "え"); r2KMap.put("xye", "え");
        r2KMap.put("tsu", "つ"); r2KMap.put("xtu", "つ"); r2KMap.put("ltu", "つ");
        r2KMap.put("xya", "や"); r2KMap.put("lya", "や"); r2KMap.put("wyi", "ゐ");
        r2KMap.put("xyu", "ゆ"); r2KMap.put("lyu", "ゆ"); r2KMap.put("xyo", "よ"); r2KMap.put("lyo", "よ");
        r2KMap.put("xwa", "わ"); r2KMap.put("lwa", "わ");

        r2KMap.put("wha", "うあ"); r2KMap.put("whi", "うい"); r2KMap.put("whu", "う"); r2KMap.put("whe", "うえ"); r2KMap.put("who", "うお");
        r2KMap.put("t'yu", "てゆ");  r2KMap.put("d'yu", "でゆ"); r2KMap.put("hwyu", "ふゆ");
        r2KMap.put("xtsu", "つ"); r2KMap.put("ltsu", "つ");

        // 最後の文字補完
        lastMap = new HashMap<CharSequence, CharSequence>();
        lastMap.put("c", "ti"); lastMap.put("f", "hu");
        lastMap.put("j", "zi"); lastMap.put("q", "ku");
        lastMap.put("v", "vu");

        lastMap.put("ky", "ki"); lastMap.put("gy", "gi"); lastMap.put("sy", "si"); lastMap.put("sh", "si");
        lastMap.put("dy", "di"); lastMap.put("ty", "ti"); lastMap.put("ts", "ti"); lastMap.put("th", "te");
        lastMap.put("dh", "de"); lastMap.put("tw", "to"); lastMap.put("dw", "do"); lastMap.put("ny", "ni");
        lastMap.put("hy", "hi"); lastMap.put("by", "bi"); lastMap.put("py", "pi"); lastMap.put("hw", "hu");
        lastMap.put("my", "mi"); lastMap.put("ry", "ri"); lastMap.put("kw", "ku"); lastMap.put("gw", "gu");
        lastMap.put("wh", "u");

        initializeBuffers();
    }

    public String[] createSearchTexts(final String source) {
        final String[] texts = new String[2];
        CharSequence base = normalize(source);
        texts[0] = base.toString();

        CharSequence changedLast = lastMap.get(
                Character.toString(base.charAt(base.length() - 1)));

        if (changedLast == null) {
            if (base.length() >= 2) {
                changedLast = lastMap.get(
                        base.subSequence(base.length() - 2, base.length()));
                if (changedLast != null) {
                    base = base.subSequence(0, base.length() - 2).toString() + changedLast;
                }
            }
        } else {
            base = base.subSequence(0, base.length() - 1).toString() + changedLast;
        }
        base = convertKana2Roma(convertRoma2Kana(convertKata2Hira(base)));
        texts[1] = base.toString();
        return texts;
    }

    public void createIndex(
            final Context c, final String source, final DatabaseHelper helper,
            final StringBuilder indexBuilder) throws SQLException {
        final CharSequence text = normalize(source);

        indexBuilder.append(text).append(" ");

        mTempBuilder1.setLength(0);
        mEnglishBuilder.setLength(0);

        if (!isHalf(text)) {
            appendDividedWords(text, helper, REG_WORD, 3, Dictionary.TYPE_JA, true);
            if (mTempBuilder1.length() == 0) {
                appendDividedWords(text, helper, REG_KANJI, 1, Dictionary.TYPE_JA, true);
            }
            if (mTempBuilder1.length() == 0) mTempBuilder1.append(text).append(" ");

            appendDividedWords(text, helper, REG_KANA, 2, Dictionary.TYPE_EN, true);
        }
        appendDividedWords(text, helper, REG_ENGLISH, 3, Dictionary.TYPE_EN, false);

        mTempBuilder2.setLength(0);
        convertKata2Hira(mTempBuilder1, mTempBuilder2);

        mTempBuilder1.setLength(0);
        convertRoma2Kana(mTempBuilder2, mTempBuilder1);
        convertKana2Roma(mTempBuilder1, indexBuilder);

        if (mEnglishBuilder.length() > 0) {
            indexBuilder.append(mEnglishBuilder.toString()).append(" ");
        }
    }

    public String normalize(final String source) {
        return Normalizer.normalize(
                source.trim().toLowerCase(Locale.ENGLISH), Normalizer.Form.NFKC);
    }

    public void clearBuffers() {
        initializeBuffers();
    }

    public boolean initializeDics(final Context c) {
        final ArrayList<Dictionary> dics = new ArrayList<Dictionary>();

        try {
            readDics(c, R.raw.dic_ja, Dictionary.TYPE_JA, dics);
            readDics(c, R.raw.dic_en, Dictionary.TYPE_EN, dics);

            final DatabaseHelper helper = DatabaseHelper.getInstance(c);
            TransactionManager.callInTransaction(
                    helper.getConnectionSource(), new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            helper.getDao(Dictionary.class).deleteBuilder().delete();

                            for (Dictionary dic : dics) {
                                helper.getDao(Dictionary.class).create(dic);
                            }
                            Log.d(TAG, "dics.size:" + dics.size());
                            return null;
                        }
            });
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private void appendDividedWords(
            final CharSequence text, final DatabaseHelper helper,
            final String reg, final int endLength, final int type, final boolean isKey)
                    throws SQLException {
        final Pattern pattern = Pattern.compile(reg);
        final Matcher matcher = pattern.matcher(text);
        final Dao<Dictionary, Integer> dao = helper.getDao(Dictionary.class);
        mTempBuilder2.setLength(0);

        int replaceChangedIndex = 0;
        while (matcher.find()) {
            String word = matcher.group();

            Dictionary dic = findDic(dao, word, type, isKey);
            if (dic == null) {
                int beginLength = word.length() - 1;
                for (int j = beginLength; j >= endLength; j--) {
                    for (int i = 0; i < beginLength - j + 2; i++) {
                        final String subWord = word.substring(i, i + j);
                        dic = findDic(dao, subWord, type, isKey);
                        if (dic == null) continue;

                        word = word.replace(subWord, isKey ? dic.value : dic.key);
                        beginLength = word.length() - 1;
                        j = beginLength;
                        i = 0;
                    }
                }
                if (!word.equals(matcher.group())) {
                    replaceWord(text, mTempBuilder2, matcher, replaceChangedIndex, word);
                    replaceChangedIndex += word.length() - matcher.end() + matcher.start();
                }
            } else {
                final String value = isKey ? dic.value : dic.key;
                replaceWord(text, mTempBuilder2, matcher, replaceChangedIndex, value);
                replaceChangedIndex += value.length() - matcher.end() + matcher.start();
            }
        }
        if (mTempBuilder2.length() > 0) {
            final String dividedWords = mTempBuilder2.toString();
            mTempBuilder1.append(dividedWords).append(" ");

            if (isHalf(dividedWords)) mEnglishBuilder.append(dividedWords).append(" ");
        }
    }

    private void replaceWord(
            final CharSequence title, final StringBuilder buffer3, final Matcher matcher,
            int replaceChangedIndex, String word) {
        if (buffer3.length() == 0) buffer3.append(title);
        buffer3.replace(
                matcher.start() + replaceChangedIndex,
                matcher.end() + replaceChangedIndex, word);
    }

    private Dictionary findDic(
            final Dao<Dictionary, Integer> dao, final String word,
            final int type, final boolean isKey) throws SQLException {
        final String keyName = isKey ? Dictionary.KEY : Dictionary.VALUE;

        return dao.queryBuilder()
                .orderBy(Dictionary.PRIORITY, true)
                .where().eq(keyName, word).and().eq(Dictionary.TYPE, type)
                .queryForFirst();
    }

    private void initializeBuffers() {
        mTempBuilder1 = new StringBuilder();
        mTempBuilder2 = new StringBuilder();
        mEnglishBuilder = new StringBuilder();
    }

    private String convertKana2Roma(final CharSequence source) {
        final StringBuilder builder = new StringBuilder();
        convertKana2Roma(source, builder);
        return builder.toString();
    }

    private void convertKana2Roma(final CharSequence source, final StringBuilder builder) {
        for (int i = 0; i < source.length(); i++) {
            final char character = source.charAt(i);
            final CharSequence roma = k2RMap.get(character);

            if (roma == null) builder.append(character);
            else builder.append(roma);
        }
    }

    private String convertRoma2Kana(final CharSequence source) {
        final StringBuilder builder = new StringBuilder();
        convertRoma2Kana(source, builder);
        return builder.toString();
    }

    private void convertRoma2Kana(final CharSequence source, final StringBuilder builder) {
        for (int j = 0; j < source.length(); j++) {
            for (int i = 4; i >= 1; i--) {
                final int end = j + i;
                if (end > source.length()) continue;

                final CharSequence roma = source.subSequence(j, end);
                final CharSequence kana = r2KMap.get(roma);
                if (kana == null) {
                    if (i == 1) builder.append(roma);
                    continue;
                }

                builder.append(kana);
                j = end - 1;
                break;
            }
        }
    }

    private static String convertKata2Hira(final CharSequence source) {
        final StringBuilder builder = new StringBuilder();
        convertKata2Hira(source, builder);
        return builder.toString();
    }

    private static String convertKata2Hira(final CharSequence source, final StringBuilder builder) {
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c >= 'ァ' && c <= 'ン') {
                builder.append((char) (c - 'ァ' + 'ぁ'));
            } else if (c == 'ヴ') {
                builder.append('ゔ');
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static boolean isHalf(final CharSequence source) {
        if (TextUtils.isEmpty(source)) return true;

        final Pattern pattern = Pattern.compile(REG_HALF);
        return pattern.matcher(source).matches();
    }

    private static void readDics(
            final Context c, final int resourceId, final int type,
            final ArrayList<Dictionary> dics) {
        InputStream is = null;
        BufferedReader reader = null;
        ZipInputStream zis = null;

        try {
            is = c.getResources().openRawResource(resourceId);
            zis = new ZipInputStream(is);
            reader = new BufferedReader(new InputStreamReader(zis));

            String line = null;
            if (zis.getNextEntry() != null) {
                while ((line = reader.readLine()) != null) {
                    final String[] words = line.split("/");
                    if (words.length < 2) continue;

                    final String value = words[0];
                    for (int i = 1; i < words.length; i++) {
                        final Dictionary dic = new Dictionary();
                        dic.key = words[i];
                        dic.value = value;
                        dic.type = type;
                        dic.priority = words.length - 1;
                        dics.add(dic);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (zis != null) zis.closeEntry();
                if (reader != null) reader.close();
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
