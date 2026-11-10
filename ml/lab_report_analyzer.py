"""
MedAssist AI - Smart Medical Lab Report Analyzer & Layman Decoder Engine
Complies with Indian clinical laboratory standards (Tietz / ICMR / NABL benchmarks).

Key Capabilities:
1. Biomarker Knowledge Base with clinical reference ranges across CBC, Diabetes, Lipids, KFT, LFT, and Thyroid.
2. Multilingual layman explanations in English, Marathi, and Hindi.
3. Robust regex-based biomarker extraction from raw pasted or OCR laboratory text.
4. Clinical status classification ('low', 'normal', 'high', 'critical_low', 'critical_high').
5. Normalized gauge percentage (0.0 to 1.0) and hex severity colors for mobile UI dashboard rendering.
6. Targeted "Questions to Ask Your Doctor", actionable dietary advice, and clinical disclaimers.
"""

import re
import logging
from typing import Dict, List, Optional, Tuple, Any, Union

logger = logging.getLogger("medassist.lab_report_analyzer")

# =============================================================================
# 1. BIOMARKER CLINICAL BENCHMARKS & MULTILINGUAL KNOWLEDGE BASE
# =============================================================================

BIOMARKER_DATABASE: Dict[str, Dict[str, Any]] = {
    "hemoglobin": {
        "canonical_name": "Hemoglobin",
        "display_name": {
            "en": "Hemoglobin (Hb)",
            "mr": "हिमोग्लोबिन (रक्तद्रव्य)",
            "hi": "हीमोग्लोबिन (रक्त प्रोटीन)",
        },
        "aliases": [
            "hemoglobin", "haemoglobin", "hb", "hgb", "total hemoglobin",
            "हिमोग्लोबिन", "हीमोग्लोबिन"
        ],
        "default_unit": "g/dL",
        "ref_min": 12.0,
        "ref_max": 16.5,
        "critical_low": 7.0,
        "critical_high": 19.0,
        "ref_str": "12.0 - 16.5 g/dL",
        "what_it_is": {
            "en": "Protein in red blood cells that carries oxygen from your lungs to the entire body.",
            "mr": "शरीरात आणि अवयवांना प्राणवायू (ऑक्सिजन) पोहोचवणारे लाल रक्तपेशींमधील मुख्य घटक.",
            "hi": "लाल रक्त कोशिकाओं में मौजूद प्रोटीन जो फेफड़ों से पूरे शरीर में ऑक्सीजन पहुंचाता है.",
        },
        "explanations": {
            "low": {
                "en": "Low hemoglobin indicates anemia. This may cause fatigue, breathlessness, dizziness, and pale skin.",
                "mr": "हिमोग्लोबिन कमी असल्याने अशक्तपणा (अॅनिमिया/पांढरे पडणे) होऊ शकतो. यामुळे थकवा, धाप लागणे आणि चक्कर येऊ शकते.",
                "hi": "हीमोग्लोबिन की कमी एनीमिया (खून की कमी) दर्शाती है। इससे कमजोरी, थकान और सांस फूलने की समस्या हो सकती है।",
            },
            "normal": {
                "en": "Hemoglobin is in the healthy range, supporting optimal oxygen delivery to tissues.",
                "mr": "हिमोग्लोबिनची पातळी योग्य आहे, शरीराला पुरेसा प्राणवायू मिळत आहे.",
                "hi": "हीमोग्लोबिन का स्तर सामान्य है, शरीर में ऑक्सीजन का प्रवाह सही है।",
            },
            "high": {
                "en": "High hemoglobin may indicate dehydration, chronic smoking, or bone marrow overproduction.",
                "mr": "हिमोग्लोबिन वाढलेले आहे. हे डिहायड्रेशन (पाण्याची कमतरता) किंवा फुफ्फुसांच्या ताणामुळे असू शकते.",
                "hi": "हीमोग्लोबिन बढ़ा हुआ है। यह निर्जलीकरण (डिहाइड्रेशन) या धूम्रपान के कारण हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Include iron-rich foods: spinach, beetroot, pomegranate, jaggery, dates, lentils, and vitamin C (lemon) to aid absorption.",
            "mr": "पालक, बीट, गूळ, खजूर, डाळिंब, अंजीर आणि डाळींचा आहारात समावेश करा; लोह शोषणासाठी लिंबू पाणी फायदेशीर ठरते.",
            "hi": "पालक, चुकंदर, गुड़, खजूर, अनार, हरी सब्जियां और दालें खाएं; नींबू पानी से आयरन का अवशोषण बेहतर होता है।",
        },
        "doctor_question": {
            "en": "Do I need iron or vitamin B12 supplementation, or further peripheral smear tests?",
            "mr": "माझे हिमोग्लोबिन कमी असल्याने मला आयर्न किंवा बी-१२ सप्लिमेंट्सची गरज आहे का?",
            "hi": "क्या मुझे आयरन या विटामिन बी12 की खुराक लेने की आवश्यकता है?",
        },
    },

    "wbc": {
        "canonical_name": "WBC Count",
        "display_name": {
            "en": "Total WBC Count (Leukocytes)",
            "mr": "पांढऱ्या रक्तपेशी (WBC)",
            "hi": "श्वेत रक्त कोशिकाएं (WBC)",
        },
        "aliases": [
            "wbc", "total wbc", "total leukocyte count", "tlc", "white blood cells",
            "leukocytes", "पांढऱ्या पेशी"
        ],
        "default_unit": "/mcL",
        "ref_min": 4000.0,
        "ref_max": 11000.0,
        "critical_low": 2000.0,
        "critical_high": 30000.0,
        "ref_str": "4,000 - 11,000 /mcL",
        "what_it_is": {
            "en": "The defense army of the immune system that fights off bacterial, viral, and fungal infections.",
            "mr": "शरीराची रोगप्रतिकारक यंत्रणा आणि संसर्गाशी लढणाऱ्या रक्षक पांढऱ्या पेशी.",
            "hi": "प्रतिरक्षा प्रणाली की मुख्य कोशिकाएं जो बैक्टीरिया और वायरस के संक्रमण से लड़ती हैं.",
        },
        "explanations": {
            "low": {
                "en": "Low WBC count (leukopenia) weakens resistance, increasing vulnerability to infections.",
                "mr": "पांढऱ्या पेशी कमी झाल्यामुळे रोगप्रतिकारशक्ती मंदावते व संसर्ग लवकर होण्याची शक्यता असते.",
                "hi": "डब्ल्यूबीसी कम होने से रोग प्रतिरोधक क्षमता कमजोर होती है और संक्रमण का खतरा बढ़ता है।",
            },
            "normal": {
                "en": "WBC count is normal, indicating an active and balanced immune system.",
                "mr": "पांढऱ्या पेशींचे प्रमाण योग्य आहे, रोगप्रतिकारशक्ती उत्तम स्थितीत आहे.",
                "hi": "डब्ल्यूबीसी की संख्या सामान्य है, प्रतिरक्षा तंत्र स्वस्थ है।",
            },
            "high": {
                "en": "Elevated WBC count suggests active infection, acute inflammation, or physical stress.",
                "mr": "पांढऱ्या पेशी वाढल्या आहेत. हे शरीरात जंतुसंसर्ग (इन्फेक्शन), ताप किंवा सूज असल्याचे दर्शवते.",
                "hi": "डब्ल्यूबीसी का बढ़ना शरीर में किसी सक्रिय संक्रमण (इन्फेक्शन) या सूजन का संकेत है।",
            },
        },
        "dietary_advice": {
            "en": "Focus on immune boosters: citrus fruits (amla, oranges), garlic, ginger, turmeric, and probiotics (curd).",
            "mr": "आवळा, संत्री, हळद, आले, लसूण आणि ताजे दही यांचा आहारात वापर करून प्रतिकारशक्ती वाढवा.",
            "hi": "आंवला, संतरा, हल्दी, अदरक, लहसुन और ताजा दही लें जिससे रोग प्रतिरोधक क्षमता बढ़े।",
        },
        "doctor_question": {
            "en": "Does my elevated WBC point to a bacterial or viral infection, and do I require antibiotics?",
            "mr": "पांढऱ्या पेशी वाढल्यामुळे मला अँटिबायोटिक्स किंवा संसर्ग प्रतिबंधक उपचारांची गरज आहे का?",
            "hi": "क्या यह बढ़ा हुआ स्तर किसी बैक्टीरिया संक्रमण के कारण है और क्या मुझे एंटीबायोटिक की जरूरत है?",
        },
    },

    "platelets": {
        "canonical_name": "Platelet Count",
        "display_name": {
            "en": "Platelet Count",
            "mr": "प्लेटलेट्स (रक्तबिंबिका)",
            "hi": "प्लेटलेट काउंट",
        },
        "aliases": [
            "platelet count", "platelets", "plt", "total platelets", "प्लेटलेट्स"
        ],
        "default_unit": "/mcL",
        "ref_min": 150000.0,
        "ref_max": 450000.0,
        "critical_low": 50000.0,
        "critical_high": 1000000.0,
        "ref_str": "150,000 - 450,000 /mcL",
        "what_it_is": {
            "en": "Blood cell fragments essential for clotting, wound sealing, and preventing bleeding.",
            "mr": "रक्त गोठण्यास मदत करणाऱ्या पेशी, ज्यामुळे जखम झाल्यावर रक्तस्राव रोखला जातो.",
            "hi": "रक्त को जमाने और चोट लगने पर खून का बहाव रोकने वाली आवश्यक कोशिकाएं.",
        },
        "explanations": {
            "low": {
                "en": "Low platelets (thrombocytopenia) elevate bleeding risk. Common in viral fevers like Dengue or Malaria.",
                "mr": "प्लेटलेट्स कमी झाल्या आहेत. डेंग्यू किंवा व्हायरल तापामध्ये असे घडू शकते; रक्तस्रावाचा धोका वाढतो.",
                "hi": "प्लेटलेट्स कम होना रक्तस्राव का जोखिम बढ़ाता है। डेंगू या वायरल बुखार में ऐसा अक्सर देखा जाता है।",
            },
            "normal": {
                "en": "Platelet levels are optimal for healthy blood clotting.",
                "mr": "प्लेटलेट्सचे प्रमाण सामान्य आणि सुरक्षित पातळीवर आहे.",
                "hi": "प्लेटलेट की संख्या सामान्य और सुरक्षित स्तर पर है।",
            },
            "high": {
                "en": "Elevated platelets (thrombocytosis) may occur in reaction to inflammation or iron deficiency.",
                "mr": "प्लेटलेट्स वाढल्या आहेत. हे शरीरातील जुनी सूज किंवा रक्तातील लोहाच्या कमतरतेमुळे असू शकते.",
                "hi": "प्लेटलेट्स का स्तर अधिक है, जो किसी पुरानी सूजन या आयरन की कमी की प्रतिक्रिया हो सकती है।",
            },
        },
        "dietary_advice": {
            "en": "Hydrate adequately with coconut water, papaya leaf extract (if advised), pomegranate, and kiwi.",
            "mr": "शहाळ्याचे पाणी, पपई, डाळिंब, किवी आणि भरपूर पातळ पदार्थांचे सेवन करा.",
            "hi": "नारियल पानी, पपीता, कीवी, अनार का सेवन करें और शरीर में पानी की कमी न होने दें।",
        },
        "doctor_question": {
            "en": "Should I repeat the platelet test in 24-48 hours to monitor the trend?",
            "mr": "प्लेटलेट्सवर लक्ष ठेवण्यासाठी २४ ते ४८ तासांत पुन्हा तपासणी करावी लागेल का?",
            "hi": "क्या मुझे प्लेटलेट की निगरानी के लिए 24 से 48 घंटे बाद दोबारा टेस्ट कराना चाहिए?",
        },
    },

    "rbc": {
        "canonical_name": "RBC Count",
        "display_name": {
            "en": "Red Blood Cell (RBC) Count",
            "mr": "लाल रक्तपेशी (RBC)",
            "hi": "लाल रक्त कोशिकाएं (RBC)",
        },
        "aliases": [
            "rbc count", "rbc", "total rbc", "red blood cells", "erythrocytes"
        ],
        "default_unit": "million/mcL",
        "ref_min": 4.2,
        "ref_max": 5.8,
        "critical_low": 2.5,
        "critical_high": 7.5,
        "ref_str": "4.2 - 5.8 mil/mcL",
        "what_it_is": {
            "en": "Cells that transport hemoglobin and vital gases across bodily organs.",
            "mr": "शरीरात ऑक्सिजन वाहून नेणाऱ्या मुख्य तांबड्या रक्तपेशी.",
            "hi": "शरीर के सभी अंगों तक ऑक्सीजन ले जाने वाली मुख्य लाल कोशिकाएं.",
        },
        "explanations": {
            "low": {
                "en": "Reduced RBC count is a core sign of anemia or blood volume loss.",
                "mr": "लाल पेशी कमी झाल्यामुळे शरीरात रक्ताची कमतरता निर्माण होते.",
                "hi": "आरबीसी की कमी खून की कमी (एनीमिया) का स्पष्ट संकेत है।",
            },
            "normal": {
                "en": "RBC count is balanced and within standard parameters.",
                "mr": "लाल रक्तपेशींचे प्रमाण अगदी योग्य आहे.",
                "hi": "आरबीसी की संख्या सामान्य स्तर पर है।",
            },
            "high": {
                "en": "High RBC count indicates polycythemia or reduced blood plasma due to dehydration.",
                "mr": "लाल पेशी वाढल्या आहेत; डिहायड्रेशन किंवा प्राणवायूची कमतरता हे कारण असू शकते.",
                "hi": "आरबीसी का बढ़ना शरीर में पानी की कमी या ऑक्सीजन की कमी का संकेत हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Consume green leafy vegetables, legumes, whole grains, and foods rich in folate and vitamin B12.",
            "mr": "हिरव्या पालेभाज्या, मोड आलेली कडधान्ये, फळे आणि बी-कॉम्प्लेक्सयुक्त अन्न खा.",
            "hi": "हरी पत्तेदार सब्जियां, अंकुरित अनाज, दालें और विटामिन बी12 युक्त खाद्य पदार्थ लें।",
        },
        "doctor_question": {
            "en": "Does this RBC value correlate with my hemoglobin and iron indices?",
            "mr": "या लाल पेशींचा संबंध माझ्या हिमोग्लोबिन आणि थकव्याच्या लक्षणांशी आहे का?",
            "hi": "क्या यह आरबीसी काउंट मेरी थकान और हीमोग्लोबिन से संबंधित है?",
        },
    },

    "glucose_fasting": {
        "canonical_name": "Fasting Blood Sugar",
        "display_name": {
            "en": "Fasting Blood Sugar (FBS)",
            "mr": "उपाशीपोटी साखर (Fasting Sugar)",
            "hi": "फास्टिंग ब्लड शुगर (खाली पेट)",
        },
        "aliases": [
            "fasting blood sugar", "fbs", "glucose fasting", "fasting glucose",
            "fasting blood glucose", "उपाशीपोटी साखर"
        ],
        "default_unit": "mg/dL",
        "ref_min": 70.0,
        "ref_max": 99.0,
        "critical_low": 55.0,
        "critical_high": 300.0,
        "ref_str": "70 - 99 mg/dL",
        "what_it_is": {
            "en": "Measures baseline circulating glucose after 8-10 hours of overnight fasting.",
            "mr": "रात्रभर ८-१० तास उपाशी राहिल्यानंतर रक्तातील साखरेचे मोजमाप.",
            "hi": "रात भर (8-10 घंटे) खाली पेट रहने के बाद रक्त में मौजूद ग्लूकोज का स्तर.",
        },
        "explanations": {
            "low": {
                "en": "Low blood sugar (hypoglycemia) can cause sweating, shakiness, confusion, and dizziness.",
                "mr": "साखर प्रमाणापेक्षा कमी झाली आहे (हायपोग्लायसेमिया). यामुळे चक्कर, घाम आणि थरथर जाणवू शकते.",
                "hi": "ब्लड शुगर कम है (हाइपोग्लाइसीमिया)। इससे चक्कर, पसीना और कंपकंपी हो सकती है।",
            },
            "normal": {
                "en": "Fasting glucose is in the ideal non-diabetic range.",
                "mr": "उपाशीपोटी साखरेचे प्रमाण पूर्णपणे सामान्य व सुरक्षित आहे.",
                "hi": "फास्टिंग ब्लड शुगर का स्तर बिल्कुल सामान्य है।",
            },
            "high": {
                "en": "Elevated fasting glucose (100-125: prediabetes; >=126: diabetes indicator). Requires medical evaluation.",
                "mr": "साखरेचे प्रमाण वाढले आहे (१००-१२५: प्रीडायबेटिस, १२६ पेक्षा जास्त: मधुमेह). डॉक्टरी सल्ला आवश्यक आहे.",
                "hi": "फास्टिंग शुगर अधिक है (100-125 प्रीडायबिटीज, 126+ डायबिटीज का संकेत)। तुरंत चिकित्सक से परामर्श लें।",
            },
        },
        "dietary_advice": {
            "en": "Avoid added sugars, sweets, and refined flour. Emphasize bitter gourd, fenugreek seeds, salads, and brisk walking.",
            "mr": "साखर, गूळ, मिठाई व बेकरी पदार्थ टाळा; मेथी दाणे, कारले, काकडी आणि रोज ३० मिनिटे वेगाने चाला.",
            "hi": "मीठा, सफेद चावल, मैदा बंद करें; मेथी, करेला, सलाद और नियमित 30 मिनट टहलने की आदत डालें।",
        },
        "doctor_question": {
            "en": "Should we confirm this with an HbA1c test or oral glucose tolerance test?",
            "mr": "या वाढलेल्या साखरेसाठी मला HbA1c टेस्ट किंवा औषधोपचार सुरू करावे लागतील का?",
            "hi": "क्या मुझे इसकी पुष्टि के लिए HbA1c टेस्ट और दवा की आवश्यकता है?",
        },
    },

    "glucose_pp": {
        "canonical_name": "Post-Prandial Blood Sugar",
        "display_name": {
            "en": "Post-Prandial Glucose (PPBS)",
            "mr": "जेवणानंतरची साखर (PPBS)",
            "hi": "भोजन के बाद की शुगर (PPBS)",
        },
        "aliases": [
            "post prandial blood sugar", "ppbs", "glucose post prandial", "postprandial glucose",
            "glucose pp", "जेवणानंतरची साखर"
        ],
        "default_unit": "mg/dL",
        "ref_min": 70.0,
        "ref_max": 140.0,
        "critical_low": 60.0,
        "critical_high": 350.0,
        "ref_str": "< 140 mg/dL",
        "what_it_is": {
            "en": "Assesses how effectively insulin processes glucose exactly 2 hours after a meal.",
            "mr": "जेवणानंतर २ तासांनी शरीर आणि इन्सुलिन साखरेचे नियंत्रण कसे करते याचे मोजमाप.",
            "hi": "भोजन करने के 2 घंटे बाद शरीर में इंसुलिन द्वारा ग्लूकोज नियंत्रण की क्षमता का परीक्षण.",
        },
        "explanations": {
            "low": {
                "en": "Post-meal glucose is unusually low, possibly due to medication timing or reactive drop.",
                "mr": "जेवणानंतरची साखर कमी झाली आहे; औषधांचा डोस तपासणे गरजेचे आहे.",
                "hi": "खाने के बाद शुगर कम है; दवाओं के समय या खुराक की जांच जरूरी है।",
            },
            "normal": {
                "en": "Post-meal glucose processing is healthy and within normal limits.",
                "mr": "जेवणानंतरची साखर नियंत्रणात असून शरीराचे इन्सुलिन कार्य सुरळीत आहे.",
                "hi": "भोजन के बाद का शुगर स्तर सामान्य और नियंत्रित है।",
            },
            "high": {
                "en": "Post-meal glucose is elevated (140-199: impaired tolerance; >=200: diabetes).",
                "mr": "जेवणानंतरची साखर वाढली आहे (१४०-१९९: असंतुलित, २०० पेक्षा जास्त: मधुमेह). आहारावर नियंत्रण ठेवा.",
                "hi": "खाने के बाद ब्लड शुगर अधिक है (140-199 अनियंत्रित, 200+ डायबिटीज)। खानपान पर नियंत्रण रखें।",
            },
        },
        "dietary_advice": {
            "en": "Limit simple carbohydrates and large rice portions; replace with fiber-rich multigrain rotis and green salads.",
            "mr": "जास्त भात व बटाटा टाळा; ज्वारी, बाजरीची भाकरी, भरपूर कोशिंबीर आणि भाज्या खा.",
            "hi": "चावल और आलू कम करें; ज्वार, बाजरा, चोकर युक्त रोटी और हरी सब्जियों की मात्रा बढ़ाएं।",
        },
        "doctor_question": {
            "en": "Do I need adjustment to my post-meal insulin or anti-diabetic tablets?",
            "mr": "जेवणानंतरची साखर नियंत्रित करण्यासाठी औषधांच्या वेळेत किंवा आहारात काय बदल करावेत?",
            "hi": "क्या खाने के बाद की शुगर कम करने के लिए दवा के डोज़ में बदलाव की जरूरत है?",
        },
    },

    "hba1c": {
        "canonical_name": "HbA1c (Glycated Hemoglobin)",
        "display_name": {
            "en": "HbA1c (3-Month Sugar Average)",
            "mr": "HbA1c (३ महिन्यांची सरासरी साखर)",
            "hi": "HbA1c (3 महीने का औसत शुगर)",
        },
        "aliases": [
            "hba1c", "glycated hemoglobin", "glycosylated hemoglobin", "a1c"
        ],
        "default_unit": "%",
        "ref_min": 4.0,
        "ref_max": 5.6,
        "critical_low": 3.5,
        "critical_high": 12.0,
        "ref_str": "< 5.7 %",
        "what_it_is": {
            "en": "Gold-standard test showing average blood glucose levels over the preceding 2 to 3 months.",
            "mr": "गेल्या ३ महिन्यांतील रक्तातील साखरेची सरासरी पातळी दर्शवणारी अत्यंत महत्त्वाची तपासणी.",
            "hi": "पिछले 3 महीनों के औसत ब्लड शुगर स्तर को दर्शाने वाला सबसे सटीक टेस्ट.",
        },
        "explanations": {
            "low": {
                "en": "HbA1c is lower than expected, which can occur with certain hemoglobin variants or hemolytic conditions.",
                "mr": "HbA1c पातळी प्रमाणापेक्षा कमी आहे; लाल पेशींची झीज किंवा अशक्तपणा तपासणे आवश्यक आहे.",
                "hi": "HbA1c का स्तर कम है, जो एनीमिया या लाल रक्त कोशिकाओं के तेजी से नष्ट होने का संकेत हो सकता है।",
            },
            "normal": {
                "en": "Excellent long-term glucose regulation (< 5.7%).",
                "mr": "गेल्या ३ महिन्यांतील साखरेचे नियंत्रण अतिशय उत्तम आणि सुरक्षित आहे (< ५.७%).",
                "hi": "पिछले 3 महीनों में शुगर का नियंत्रण बहुत अच्छा और सुरक्षित रहा है (< 5.7%)।",
            },
            "high": {
                "en": "Elevated HbA1c (5.7-6.4%: prediabetes; >=6.5%: diabetes). Long-term control needed to protect kidneys, eyes, and nerves.",
                "mr": "HbA1c वाढले आहे (५.७-६.४%: प्रीडायबेटिस, ६.५%+: मधुमेह). डोळे, नसा व किडनीच्या सुरक्षेसाठी नियंत्रण आवश्यक आहे.",
                "hi": "HbA1c बढ़ा हुआ है (5.7-6.4% प्रीडायबिटीज, 6.5%+ डायबिटीज)। आंख, गुर्दे और दिल की सुरक्षा के लिए इलाज जरूरी है।",
            },
        },
        "dietary_advice": {
            "en": "Adopt a low-glycemic Mediterranean or high-fiber Indian diet: millets (jowar, ragi), pulses, and daily 45-min exercise.",
            "mr": "ज्वारी, नाचणी, कडधान्ये खा; गोड पूर्ण बंद करा आणि दररोज किमान ४० मिनिटे व्यायाम करा.",
            "hi": "मिलेट्स (ज्वार, रागी), दालें, हरी सब्जियां खाएं; मीठा बंद करें और रोजाना 40 मिनट व्यायाम करें।",
        },
        "doctor_question": {
            "en": "What is my personalized target HbA1c, and do we need to start or intensify therapy?",
            "mr": "माझ्या वयानुसार सुरक्षित HbA1c चे ध्येय काय असावे आणि कोणती औषधे सुरू करावी लागतील?",
            "hi": "मेरे लिए सुरक्षित HbA1c का लक्ष्य क्या होना चाहिए और क्या मुझे नई दवा की आवश्यकता है?",
        },
    },

    "cholesterol_total": {
        "canonical_name": "Total Cholesterol",
        "display_name": {
            "en": "Total Cholesterol",
            "mr": "एकूण कोलेस्ट्रॉल (Total Cholesterol)",
            "hi": "कुल कोलेस्ट्रॉल (Total Cholesterol)",
        },
        "aliases": [
            "total cholesterol", "serum cholesterol", "cholesterol total", "cholesterol", "कोलेस्ट्रॉल"
        ],
        "default_unit": "mg/dL",
        "ref_min": 125.0,
        "ref_max": 200.0,
        "critical_low": 80.0,
        "critical_high": 350.0,
        "ref_str": "< 200 mg/dL",
        "what_it_is": {
            "en": "Total amount of cholesterol fats circulating in the bloodstream.",
            "mr": "रक्तातील एकूण स्निग्ध घटक (चरबी).",
            "hi": "रक्त में मौजूद कुल वसा (फैट) का स्तर.",
        },
        "explanations": {
            "low": {
                "en": "Unusually low cholesterol may suggest severe malnutrition, liver dysfunction, or hyperthyroidism.",
                "mr": "कोलेस्ट्रॉल प्रमाणापेक्षा कमी आहे; यकृत विकार किंवा कुपोषणाची शक्यता तपासावी.",
                "hi": "कोलेस्ट्रॉल असामान्य रूप से कम है; कुपोषण या लिवर की कार्यप्रणाली की जांच जरूरी है।",
            },
            "normal": {
                "en": "Total cholesterol is in the desirable cardiovascular range (< 200 mg/dL).",
                "mr": "एकूण कोलेस्ट्रॉलचे प्रमाण हृदय आणि रक्तवाहिन्यांसाठी सुरक्षित पातळीवर आहे.",
                "hi": "कोलेस्ट्रॉल का स्तर हृदय के लिए सामान्य और सुरक्षित सीमा में है।",
            },
            "high": {
                "en": "Elevated cholesterol (> 200 mg/dL) contributes to plaque buildup in heart arteries and stroke risk.",
                "mr": "कोलेस्ट्रॉल वाढले आहे (> २००). यामुळे रक्तवाहिन्यांमध्ये अडथळा निर्माण होऊन हृदयविकाराचा धोका वाढू शकतो.",
                "hi": "कोलेस्ट्रॉल बढ़ा हुआ है (> 200)। इससे धमनियों में रुकावट और दिल की बीमारी का खतरा बढ़ता है।",
            },
        },
        "dietary_advice": {
            "en": "Eliminate deep-fried items, vanaspati/dalda, and trans fats. Include garlic, walnuts, flaxseeds, and oats.",
            "mr": "तळलेले पदार्थ, डालडा, बेकरी प्रॉडक्ट्स टाळा; लसूण, अक्रोड, जवस आणि ओट्सचा वापर करा.",
            "hi": "तली-भुनी चीजें, डालडा, रिफाइंड तेल छोड़ें; लहसुन, अखरोट, अलसी और ओट्स का सेवन करें।",
        },
        "doctor_question": {
            "en": "Do my total cholesterol and lipid breakdown warrant a statin prescription?",
            "mr": "वाढलेले कोलेस्ट्रॉल कमी करण्यासाठी मला स्टेटिन (Statin) गोळ्यांची गरज आहे का?",
            "hi": "क्या कोलेस्ट्रॉल कम करने के लिए मुझे स्टेटिन दवा शुरू करने की आवश्यकता है?",
        },
    },

    "triglycerides": {
        "canonical_name": "Triglycerides",
        "display_name": {
            "en": "Triglycerides",
            "mr": "ट्रायग्लिसराइड्स (रक्तातील चरबी)",
            "hi": "ट्राइग्लिसराइड्स",
        },
        "aliases": [
            "triglycerides", "triglyceride", "tg", "tgl", "serum triglycerides"
        ],
        "default_unit": "mg/dL",
        "ref_min": 50.0,
        "ref_max": 150.0,
        "critical_low": 30.0,
        "critical_high": 500.0,
        "ref_str": "< 150 mg/dL",
        "what_it_is": {
            "en": "Most common type of fat derived from excess calories, alcohol, and refined sugars.",
            "mr": "अन्नातील जादा कॅलरीज आणि गोड पदार्थांमुळे रक्तात तयार होणारी चरबी.",
            "hi": "अतिरिक्त कैलोरी, चीनी और तेल से बनने वाला रक्त में मौजूद फैट.",
        },
        "explanations": {
            "low": {
                "en": "Low triglycerides are uncommon and generally indicate low-fat dietary intake or malabsorption.",
                "mr": "ट्रायग्लिसराइड्स कमी आहेत, हे सामान्यतः आहारातील कमी चरबीमुळे असते.",
                "hi": "ट्राइग्लिसराइड्स का स्तर कम है, जो आमतौर पर कम वसा वाले आहार के कारण होता है।",
            },
            "normal": {
                "en": "Triglyceride levels are well within the safe cardiovascular range.",
                "mr": "ट्रायग्लिसराइड्सचे प्रमाण अगदी योग्य आणि सुरक्षित आहे.",
                "hi": "ट्राइग्लिसराइड्स का स्तर हृदय के स्वास्थ्य के लिए अनुकूल है।",
            },
            "high": {
                "en": "High triglycerides (> 150 mg/dL) thicken arteries and increase risk of pancreatitis and heart disease.",
                "mr": "ट्रायग्लिसराइड्स वाढले आहेत (> १५०). गोड, तेलकट अन्न आणि दारूमुळे हे वाढते; हृदयावर ताण येतो.",
                "hi": "ट्राइग्लिसराइड्स बढ़े हुए हैं (> 150)। मीठा, शराब और तला भोजन कम करें, यह दिल के लिए हानिकारक है।",
            },
        },
        "dietary_advice": {
            "en": "Stop alcohol consumption, cut sweets and refined grains; do aerobic exercise for 30-45 minutes daily.",
            "mr": "मद्यपान पूर्ण बंद करा; गोड पदार्थ टाळा आणि दररोज नियमित व्यायाम करा.",
            "hi": "शराब का सेवन बंद करें, मीठा और रिफाइंड चीजें छोड़ें और रोजाना व्यायाम करें।",
        },
        "doctor_question": {
            "en": "Could high triglycerides be linked to metabolic syndrome or insulin resistance?",
            "mr": "माझे ट्रायग्लिसराइड्स वाढण्याचे कारण मधुमेहाची सुरुवात किंवा फॅटी लिव्हर आहे का?",
            "hi": "क्या ट्राइग्लिसराइड्स बढ़ने का संबंध फैटी लिवर या डायबिटीज से है?",
        },
    },

    "ldl": {
        "canonical_name": "LDL Cholesterol",
        "display_name": {
            "en": "LDL ('Bad' Cholesterol)",
            "mr": "एलडीएल (हानिकारक कोलेस्ट्रॉल)",
            "hi": "एलडीएल (खराब कोलेस्ट्रॉल)",
        },
        "aliases": [
            "ldl cholesterol", "ldl", "bad cholesterol", "ldl-c", "serum ldl"
        ],
        "default_unit": "mg/dL",
        "ref_min": 60.0,
        "ref_max": 100.0,
        "critical_low": 40.0,
        "critical_high": 250.0,
        "ref_str": "< 100 mg/dL",
        "what_it_is": {
            "en": "'Bad' cholesterol that deposits fatty plaques directly on blood vessel walls.",
            "mr": "रक्तवाहिन्यांच्या भिंतींवर साचून अडथळा निर्माण करणारे 'वाईट' कोलेस्ट्रॉल.",
            "hi": "'खराब' कोलेस्ट्रॉल जो रक्त की धमनियों में जमकर रुकावट पैदा करता है.",
        },
        "explanations": {
            "low": {
                "en": "Low LDL is generally protective against coronary artery blockages.",
                "mr": "एलडीएल कमी असणे हृदयाच्या आरोग्यासाठी उत्तम मानले जाते.",
                "hi": "एलडीएल का कम होना हृदय के लिए फायदेमंद माना जाता है।",
            },
            "normal": {
                "en": "LDL is in the optimal protective zone (< 100 mg/dL).",
                "mr": "एलडीएल सुरक्षित मर्यादेत आहे, रक्तवाहिन्या निरोगी राहण्यास मदत होते.",
                "hi": "एलडीएल का स्तर नियंत्रित और सुरक्षित सीमा में है।",
            },
            "high": {
                "en": "High LDL (> 100 mg/dL) accelerates arterial narrowing and increases heart attack hazard.",
                "mr": "हानिकारक एलडीएल कोलेस्ट्रॉल वाढले आहे; रक्तवाहिन्या अरुंद होऊन हार्ट अटॅकचा धोका वाढू शकतो.",
                "hi": "खराब कोलेस्ट्रॉल अधिक है। इससे रक्त धमनियों में रुकावट और हार्ट अटैक का खतरा बढ़ता है।",
            },
        },
        "dietary_advice": {
            "en": "Avoid saturated fats (butter, ghee, cheese, processed meats); consume soluble fiber like psyllium husk (isabgol) and oats.",
            "mr": "तूप, बटर, चीज आणि बेकरीचे पदार्थ कमी करा; इसबगोल, सफरचंद व ओट्सचा वापर करा.",
            "hi": "मक्खन, घी, पनीर और तली चीजें सीमित करें; इसबगोल, सेब और फाइबर युक्त आहार लें।",
        },
        "doctor_question": {
            "en": "Given my cardiovascular risk profile, what is my ideal target LDL level?",
            "mr": "माझ्या हृदयाच्या सुरक्षेसाठी एलडीएल १०० च्या आत ठेवण्यासाठी काय उपचार करावेत?",
            "hi": "मेरे हृदय स्वास्थ्य के अनुसार एलडीएल का सही लक्ष्य क्या होना चाहिए?",
        },
    },

    "hdl": {
        "canonical_name": "HDL Cholesterol",
        "display_name": {
            "en": "HDL ('Good' Cholesterol)",
            "mr": "एचडीएल (चांगले कोलेस्ट्रॉल)",
            "hi": "एचडीएल (अच्छा कोलेस्ट्रॉल)",
        },
        "aliases": [
            "hdl cholesterol", "hdl", "good cholesterol", "hdl-c", "serum hdl"
        ],
        "default_unit": "mg/dL",
        "ref_min": 40.0,
        "ref_max": 60.0,
        "critical_low": 20.0,
        "critical_high": 120.0,
        "ref_str": "> 40 mg/dL (Ideal > 50)",
        "what_it_is": {
            "en": "'Good' protective cholesterol that sweeps excess fats out of arteries back to the liver for clearance.",
            "mr": "रक्तवाहिन्यांमधील जादा चरबी यकृताकडे वाहून नेणारे 'चांगले व संरक्षक' कोलेस्ट्रॉल.",
            "hi": "'अच्छा' कोलेस्ट्रॉल जो धमनियों से अतिरिक्त फैट साफ करके लिवर तक पहुंचाता है.",
        },
        "explanations": {
            "low": {
                "en": "Low HDL (< 40 mg/dL) weakens arterial defense and elevates cardiovascular vulnerability.",
                "mr": "चांगले कोलेस्ट्रॉल कमी आहे (< ४०). यामुळे रक्तवाहिन्यांचे संरक्षण कमी होते.",
                "hi": "अच्छा कोलेस्ट्रॉल कम है (< 40)। इससे धमनियों की सुरक्षा घटती है और दिल का जोखिम बढ़ता है।",
            },
            "normal": {
                "en": "HDL is in the healthy protective range.",
                "mr": "चांगल्या कोलेस्ट्रॉलचे प्रमाण योग्य असून हृदयाला संरक्षण मिळते.",
                "hi": "एचडीएल का स्तर सामान्य है, जो हृदय की सुरक्षा के लिए अच्छा है।",
            },
            "high": {
                "en": "High HDL is strongly cardio-protective and desirable.",
                "mr": "एचडीएल उत्तम पातळीवर आहे; हे हृदयासाठी अत्यंत फायदेशीर आहे.",
                "hi": "एचडीएल का अधिक स्तर दिल की बीमारियों से बचाव में बहुत मददगार है।",
            },
        },
        "dietary_advice": {
            "en": "Boost HDL with regular brisk aerobic exercise, almonds, walnuts, chia seeds, and virgin olive/mustard oil.",
            "mr": "दररोज जलद चाला; बदाम, अक्रोड, जवस आणि मोहरीच्या तेलाचा आहारात वापर करा.",
            "hi": "नियमित तेज गति से टहलें; बादाम, अखरोट, अलसी और सरसों के तेल का उपयोग करें।",
        },
        "doctor_question": {
            "en": "How can I raise my protective HDL through exercise and lifestyle modifications?",
            "mr": "चांगले एचडीएल कोलेस्ट्रॉल वाढवण्यासाठी व्यायामाचे कोणते नियोजन करावे?",
            "hi": "अच्छा कोलेस्ट्रॉल बढ़ाने के लिए मुझे कौन से विशेष व्यायाम करने चाहिए?",
        },
    },

    "creatinine": {
        "canonical_name": "Serum Creatinine",
        "display_name": {
            "en": "Serum Creatinine (Kidney Function)",
            "mr": "सिरम क्रिएटिनिन (किडनी कार्यक्षमता)",
            "hi": "सीरम क्रिएटिनिन (गुर्दे की कार्यप्रणाली)",
        },
        "aliases": [
            "serum creatinine", "creatinine", "creat", "cr", "s.creatinine", "क्रिएटिनिन"
        ],
        "default_unit": "mg/dL",
        "ref_min": 0.6,
        "ref_max": 1.2,
        "critical_low": 0.3,
        "critical_high": 4.0,
        "ref_str": "0.6 - 1.2 mg/dL",
        "what_it_is": {
            "en": "Muscle breakdown waste filtered exclusively by the kidneys; the premier clinical marker of filtration health.",
            "mr": "स्नायूंच्या कार्यातून तयार होणारा टाकाऊ घटक, जो किडनी गाळून लघवीवाटे बाहेर टाकते.",
            "hi": "मांसपेशियों के अपशिष्ट का उत्पाद जिसे किडनी छानकर शरीर से बाहर निकालती है.",
        },
        "explanations": {
            "low": {
                "en": "Low creatinine is typically harmless, linked to reduced muscle mass or strict low-protein diets.",
                "mr": "क्रिएटिनिन कमी असणे सहसा स्नायूंची कमतरता किंवा आहारात प्रोटीन कमी असल्यामुळे होते.",
                "hi": "क्रिएटिनिन का स्तर कम होना आमतौर पर मांसपेशियों की कमी या कम प्रोटीन आहार के कारण होता है।",
            },
            "normal": {
                "en": "Creatinine is in the healthy range, indicating competent renal filtration.",
                "mr": "क्रिएटिनिन सामान्य आहे, किडनीची गाळण क्षमता सुरळीत कार्यरत आहे.",
                "hi": "क्रिएटिनिन सामान्य है, गुर्दे (किडनी) स्वस्थ और ठीक से काम कर रहे हैं।",
            },
            "high": {
                "en": "High creatinine indicates impaired kidney filtration, acute dehydration, or heavy protein/creatine intake.",
                "mr": "क्रिएटिनिन वाढले आहे. हे किडनीवर ताण, गाळण क्षमतेत अडथळा किंवा डिहायड्रेशन दर्शवते. त्वरित तपासणी करा.",
                "hi": "क्रिएटिनिन बढ़ा हुआ है। यह किडनी की कार्यक्षमता में कमी या पानी की कमी का संकेत है। डॉक्टर को दिखाएं।",
            },
        },
        "dietary_advice": {
            "en": "Drink plenty of water (unless on strict fluid restriction); avoid painkiller pills (NSAIDs) and excess red meat.",
            "mr": "भरपूर पाणी प्या; डॉक्टरांच्या सल्ल्याशिवाय पेनकिलर (वेदनानाशक गोळ्या) घेणे पूर्णपणे टाळा.",
            "hi": "पर्याप्त पानी पिएं; बिना डॉक्टर की सलाह के दर्द निवारक (पेनकिलर) दवाइयां बिल्कुल न लें।",
        },
        "doctor_question": {
            "en": "Should we calculate my eGFR (estimated filtration rate) and check urine microalbumin?",
            "mr": "किडनीची नेमकी क्षमता तपासण्यासाठी eGFR किंवा युरिन टेस्ट करण्याची गरज आहे का?",
            "hi": "क्या मुझे eGFR और यूरिन टेस्ट कराकर किडनी की सटीक स्थिति जांचनी चाहिए?",
        },
    },

    "blood_urea": {
        "canonical_name": "Blood Urea",
        "display_name": {
            "en": "Blood Urea / BUN",
            "mr": "ब्लड युरिया (Blood Urea)",
            "hi": "ब्लड यूरिया (Blood Urea)",
        },
        "aliases": [
            "blood urea", "urea", "bun", "blood urea nitrogen", "युरिया"
        ],
        "default_unit": "mg/dL",
        "ref_min": 15.0,
        "ref_max": 40.0,
        "critical_low": 5.0,
        "critical_high": 120.0,
        "ref_str": "15 - 40 mg/dL",
        "what_it_is": {
            "en": "Waste product formed in the liver when protein is metabolized and excreted via kidneys.",
            "mr": "प्रथिनांच्या पचनानंतर यकृतात तयार होणारा आणि किडनीमार्फत बाहेर टाकला जाणारा घटक.",
            "hi": "प्रोटीन के पचने के बाद लिवर में बनने वाला अपशिष्ट जिसे किडनी बाहर निकालती है.",
        },
        "explanations": {
            "low": {
                "en": "Low blood urea may reflect low protein intake, overhydration, or severe liver disease.",
                "mr": "युरिया कमी आहे, हे आहारातील प्रथिनांची कमतरता किंवा यकृताच्या ताणामुळे असू शकते.",
                "hi": "यूरिया कम होना आहार में प्रोटीन की कमी या लिवर की कमजोरी का संकेत हो सकता है।",
            },
            "normal": {
                "en": "Blood urea is within standard clinical limits.",
                "mr": "ब्लड युरियाचे प्रमाण योग्य पातळीवर आहे.",
                "hi": "ब्लड यूरिया का स्तर सामान्य और सुरक्षित है।",
            },
            "high": {
                "en": "High urea reflects dehydration, high dietary protein, gastrointestinal bleeding, or kidney impairment.",
                "mr": "ब्लड युरिया वाढले आहे; डिहायड्रेशन किंवा किडनीच्या कार्यात अडथळा हे कारण असू शकते.",
                "hi": "यूरिया बढ़ा हुआ है; पानी की कमी या किडनी पर दबाव इसका कारण हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Ensure proper hydration; maintain a balanced moderate protein intake under doctor supervision.",
            "mr": "पुरेसे पाणी प्या आणि आहारात प्रथिनांचे प्रमाण संतुलित ठेवा.",
            "hi": "खूब पानी पिएं और अत्यधिक प्रोटीन युक्त खाद्य पदार्थों का संतुलन बनाए रखें।",
        },
        "doctor_question": {
            "en": "Does my elevated urea suggest dehydration or renal clearance decline?",
            "mr": "युरिया वाढणे पाण्याच्या कमतरतेमुळे आहे की किडनीच्या समस्येमुळे?",
            "hi": "क्या यह बढ़ा हुआ यूरिया डिहाइड्रेशन के कारण है या किडनी की वजह से?",
        },
    },

    "uric_acid": {
        "canonical_name": "Serum Uric Acid",
        "display_name": {
            "en": "Serum Uric Acid (Joint Health)",
            "mr": "युरिक अॅसिड (Uric Acid)",
            "hi": "यूरिक एसिड (Uric Acid)",
        },
        "aliases": [
            "uric acid", "serum uric acid", "s.uric acid", "युरिक अॅसिड"
        ],
        "default_unit": "mg/dL",
        "ref_min": 3.5,
        "ref_max": 7.2,
        "critical_low": 1.5,
        "critical_high": 12.0,
        "ref_str": "3.5 - 7.2 mg/dL",
        "what_it_is": {
            "en": "Byproduct of purine breakdown; crystallization causes severe joint inflammation (Gout) and kidney stones.",
            "mr": "प्युरिनयुक्त अन्न पचल्यावर तयार होणारे अॅसिड; हे वाढल्यास सांधेदुखी (गठिया/Gout) किंवा मुतखडा होऊ शकतो.",
            "hi": "प्यूरीन के पचने से बनने वाला एसिड; बढ़ने पर जोड़ों में दर्द (गठिया) और पथरी की समस्या होती है.",
        },
        "explanations": {
            "low": {
                "en": "Low uric acid is uncommon and generally clinically benign.",
                "mr": "युरिक अॅसिड कमी आहे, सहसा यामुळे कोणतीही अडचण येत नाही.",
                "hi": "यूरिक एसिड कम होना आमतौर पर किसी बीमारी का संकेत नहीं होता।",
            },
            "normal": {
                "en": "Uric acid is in the safe range with minimal risk of joint crystal deposition.",
                "mr": "युरिक अॅसिडचे प्रमाण सामान्य आहे, सांध्यांमध्ये खडे साचण्याचा धोका नाही.",
                "hi": "यूरिक एसिड का स्तर सामान्य है, जोड़ों में दर्द का जोखिम नहीं है।",
            },
            "high": {
                "en": "Elevated uric acid (> 7.2 mg/dL) precipitates painful joint attacks (especially big toe) and kidney stones.",
                "mr": "युरिक अॅसिड वाढले आहे (> ७.२). यामुळे पायाचे अंगठे, सांधे सुजणे आणि मुतखडा (Kidney Stone) होण्याचा धोका असतो.",
                "hi": "यूरिक एसिड बढ़ा हुआ है (> 7.2)। इससे पैर के अंगूठे व जोड़ों में तेज दर्द और पथरी का खतरा रहता है।",
            },
        },
        "dietary_advice": {
            "en": "Avoid alcohol, beer, red meat, seafood, organ meats, and sugary syrups; drink 3-4 liters of water daily.",
            "mr": "मद्यपान, बिअर, मटण, मासे आणि बेकरी पदार्थ टाळा; दररोज ३ ते ४ लिटर पाणी प्या.",
            "hi": "शराब, बीयर, मीट और दालों की अधिक मात्रा से बचें; दिन में कम से कम 3-4 लीटर पानी पिएं।",
        },
        "doctor_question": {
            "en": "Do I need uric-acid lowering medication (such as Febuxostat or Allopurinol) to prevent joint flares?",
            "mr": "सांधेदुखी आणि मुतखडा टाळण्यासाठी मला युरिक अॅसिड कमी करणाऱ्या औषधांची गरज आहे का?",
            "hi": "क्या जोड़ों के दर्द और पथरी से बचने के लिए मुझे यूरिक एसिड कम करने की दवा चाहिए?",
        },
    },

    "bilirubin_total": {
        "canonical_name": "Total Bilirubin",
        "display_name": {
            "en": "Total Bilirubin (Liver & Jaundice)",
            "mr": "एकूण बिलिरुबिन (कावीळ तपासणी)",
            "hi": "कुल बिलीरुबिन (पीलिया जांच)",
        },
        "aliases": [
            "total bilirubin", "bilirubin total", "serum bilirubin", "s.bilirubin", "कावीळ"
        ],
        "default_unit": "mg/dL",
        "ref_min": 0.2,
        "ref_max": 1.2,
        "critical_low": 0.0,
        "critical_high": 15.0,
        "ref_str": "0.2 - 1.2 mg/dL",
        "what_it_is": {
            "en": "Yellow bile pigment produced during normal breakdown of red blood cells, processed by liver.",
            "mr": "लाल पेशींच्या विघटनातून तयार होणारा पिवळा घटक, जो यकृत प्रक्रियेद्वारे शरीराबाहेर टाकते.",
            "hi": "लाल रक्त कोशिकाओं के टूटने से बनने वाला पीला पिगमेंट जिसे लिवर बाहर निकालता है.",
        },
        "explanations": {
            "low": {
                "en": "Low bilirubin has no known adverse clinical significance.",
                "mr": "बिलिरुबिन कमी असणे चिंताजनक नाही.",
                "hi": "बिलीरुबिन का कम होना किसी बीमारी का संकेत नहीं है।",
            },
            "normal": {
                "en": "Bilirubin level is healthy; no signs of jaundice or bile duct obstruction.",
                "mr": "बिलिरुबिन सामान्य आहे, कावीळ (Jaundice) किंवा यकृताच्या आजाराचे लक्षण नाही.",
                "hi": "बिलीरुबिन सामान्य है, पीलिया (जॉन्डिस) या लिवर की खराबी का कोई संकेत नहीं है।",
            },
            "high": {
                "en": "High bilirubin (> 1.2 mg/dL) causes yellowing of eyes/skin (Jaundice), indicating hepatitis or gallstones.",
                "mr": "बिलिरुबिन वाढले आहे (> १.२). यामुळे डोळे व लघवी पिवळी होणे (कावीळ) होऊ शकते; यकृताची तपासणी तातडीने करा.",
                "hi": "बिलीरुबिन बढ़ा हुआ है (> 1.2)। इससे आंखें और त्वचा पीली पड़ सकती है (पीलिया)। तुरंत डॉक्टर से परामर्श लें।",
            },
        },
        "dietary_advice": {
            "en": "Eat light, boiled, easily digestible food; drink sugarcane juice (cleanly prepared) or coconut water; avoid oily/spicy foods and alcohol.",
            "mr": "हलके, उकडलेले अन्न खा; तेलकट, तिखट पदार्थ आणि दारू पूर्ण बंद ठेवा; उसाचा रस किंवा शहाळ्याचे पाणी प्या.",
            "hi": "हल्का, उबला खाना खाएं; तेल, मसाले और शराब बिल्कुल बंद करें; गन्ने का ताजा रस या नारियल पानी लें।",
        },
        "doctor_question": {
            "en": "Does elevated bilirubin require an ultrasound of the liver and gallbladder?",
            "mr": "कावीळ किंवा यकृतातील अडथळा तपासण्यासाठी मला पोटाची सोनोग्राफी (USG) करावी लागेल का?",
            "hi": "क्या पीलिया या पित्त की थैली में रुकावट की जांच के लिए पेट का अल्ट्रासाउंड जरूरी है?",
        },
    },

    "sgpt_alt": {
        "canonical_name": "SGPT / ALT",
        "display_name": {
            "en": "SGPT / ALT (Liver Enzyme)",
            "mr": "एसजीपीटी / एएलटी (यकृत विकर)",
            "hi": "एसजीपीटी / एएलटी (लिवर एंजाइम)",
        },
        "aliases": [
            "sgpt", "alt", "alanine aminotransferase", "sgpt/alt", "alanine transaminase"
        ],
        "default_unit": "U/L",
        "ref_min": 7.0,
        "ref_max": 55.0,
        "critical_low": 2.0,
        "critical_high": 1000.0,
        "ref_str": "7 - 55 U/L",
        "what_it_is": {
            "en": "Key enzyme found predominantly inside liver cells; leaks into blood when liver cells suffer injury or stress.",
            "mr": "यकृताच्या पेशींमध्ये आढळणारे विकर; यकृताला इजा किंवा सूज आल्यास हे रक्तात मिसळते.",
            "hi": "लिवर की कोशिकाओं में मौजूद एंजाइम; लिवर पर सूजन या चोट लगने पर यह खून में बढ़ जाता है.",
        },
        "explanations": {
            "low": {
                "en": "Low SGPT/ALT is normal and clinically insignificant.",
                "mr": "एसजीपीटी कमी असणे सामान्य आहे.",
                "hi": "एसजीपीटी का कम स्तर सामान्य माना जाता है।",
            },
            "normal": {
                "en": "SGPT/ALT is normal, reflecting healthy liver cell integrity.",
                "mr": "एसजीपीटी सामान्य आहे, यकृताच्या पेशी निरोगी आहेत.",
                "hi": "एसजीपीटी सामान्य है, लिवर की कोशिकाएं स्वस्थ हैं।",
            },
            "high": {
                "en": "Elevated SGPT/ALT flags liver cell inflammation, fatty liver, viral hepatitis, or medication toxicity.",
                "mr": "एसजीपीटी वाढले आहे. हे फॅटी लिव्हर (Fatty Liver), यकृतावरील सूज किंवा औषधांच्या दुष्परिणामांचे लक्षण असू शकते.",
                "hi": "एसजीपीटी बढ़ा हुआ है। यह फैटी लिवर, हेपेटाइटिस या दवाइयों के दुष्प्रभाव का संकेत हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Avoid all alcohol, sugary sodas, and fried snacks; take black coffee, green tea, and fresh vegetables.",
            "mr": "दारू पूर्ण बंद करा; तळलेले व मैद्याचे पदार्थ टाळा; ग्रीन टी आणि ताजी फळे आहारात घ्या.",
            "hi": "शराब और जंक फूड का पूरी तरह परहेज करें; वजन नियंत्रित रखें और हरी सब्जियां खाएं।",
        },
        "doctor_question": {
            "en": "Could this enzyme elevation be linked to Fatty Liver Disease or recent medications?",
            "mr": "माझे लिव्हर एन्झाइम्स वाढणे हे फॅटी लिव्हरमुळे आहे का, आणि मला औषधोपचारांची गरज आहे का?",
            "hi": "क्या इस बढ़े हुए स्तर का कारण फैटी लिवर या कोई दवा है?",
        },
    },

    "sgot_ast": {
        "canonical_name": "SGOT / AST",
        "display_name": {
            "en": "SGOT / AST (Liver & Muscle Enzyme)",
            "mr": "एसजीओटी / एएसटी (यकृत व स्नायू विकर)",
            "hi": "एसजीओटी / एएसटी (लिवर एंजाइम)",
        },
        "aliases": [
            "sgot", "ast", "aspartate aminotransferase", "sgot/ast", "aspartate transaminase"
        ],
        "default_unit": "U/L",
        "ref_min": 8.0,
        "ref_max": 48.0,
        "critical_low": 2.0,
        "critical_high": 1000.0,
        "ref_str": "8 - 48 U/L",
        "what_it_is": {
            "en": "Enzyme present in liver, heart muscle, and skeletal muscle cells.",
            "mr": "यकृत, हृदय आणि स्नायूंमध्ये आढळणारे महत्त्वाचे विकर.",
            "hi": "लिवर और मांसपेशियों में पाया जाने वाला एंजाइम.",
        },
        "explanations": {
            "low": {
                "en": "Low SGOT/AST is benign and normal.",
                "mr": "एसजीओटी कमी असणे सामान्य आहे.",
                "hi": "एसजीओटी का कम होना सामान्य है।",
            },
            "normal": {
                "en": "SGOT/AST levels reflect sound cellular health.",
                "mr": "एसजीओटी योग्य पातळीवर आहे.",
                "hi": "एसजीओटी का स्तर सामान्य है।",
            },
            "high": {
                "en": "Elevated SGOT/AST suggests liver stress, heavy alcohol exposure, strenuous workouts, or muscle injury.",
                "mr": "एसजीओटी वाढले आहे; यकृतावर ताण, अतिव्यायाम किंवा मद्यपानामुळे हे घडू शकते.",
                "hi": "एसजीओटी बढ़ा हुआ है; लिवर पर दबाव, शराब या मांसपेशियों में चोट इसका कारण हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Rest adequately, hydrate, strictly avoid alcohol and unverified supplements.",
            "mr": "पुरेशी विश्रांती घ्या, भरपूर पाणी प्या आणि मद्यपान टाळा.",
            "hi": "पर्याप्त आराम करें, पानी पिएं और शराब व अवांछित सप्लीमेंट्स से बचें।",
        },
        "doctor_question": {
            "en": "Does my AST/ALT ratio suggest alcohol-related or non-alcoholic fatty liver changes?",
            "mr": "एसजीओटी आणि एसजीपीटी यांच्या गुणोत्तरावरून लिव्हरच्या आरोग्याबाबत काय समजते?",
            "hi": "क्या एएसटी और एएलटी के अनुपात से लिवर की स्थिति के बारे में अधिक जानकारी मिल सकती है?",
        },
    },

    "tsh": {
        "canonical_name": "TSH (Thyroid Stimulating Hormone)",
        "display_name": {
            "en": "TSH (Thyroid Stimulating Hormone)",
            "mr": "टी.एस.एच. (थायरॉईड संप्रेरक)",
            "hi": "टी.एस.एच. (थायराइड प्रेरक हार्मोन)",
        },
        "aliases": [
            "tsh", "thyroid stimulating hormone", "ultrasensitive tsh", "थायरॉईड"
        ],
        "default_unit": "mIU/L",
        "ref_min": 0.4,
        "ref_max": 4.5,
        "critical_low": 0.05,
        "critical_high": 25.0,
        "ref_str": "0.4 - 4.5 mIU/L",
        "what_it_is": {
            "en": "Pituitary hormone that instructs the thyroid gland how much metabolic hormone (T3/T4) to produce.",
            "mr": "मेंदूतील ग्रंथीकडून थायरॉईडला चयापचय (Metabolism) नियंत्रित करण्याचे आदेश देणारे संप्रेरक.",
            "hi": "पिट्यूटरी ग्रंथि द्वारा थायराइड को मेटाबॉलिज्म नियंत्रित करने का निर्देश देने वाला हार्मोन.",
        },
        "explanations": {
            "low": {
                "en": "Low TSH (< 0.4 mIU/L) suggests an overactive thyroid (Hyperthyroidism). Causes weight loss, tremors, palpitations.",
                "mr": "टी.एस.एच. कमी आहे (< ०.४); थायरॉईड अतिसक्रिय असणे (Hyperthyroidism). यामुळे वजन घटणे, छातीत धडधड होऊ शकते.",
                "hi": "टीएसएच कम है (< 0.4); थायराइड का अत्यधिक सक्रिय होना (हाइपरथायरायडिज्म)। इससे वजन घटना और धड़कन तेज होना हो सकता है।",
            },
            "normal": {
                "en": "TSH is balanced; thyroid gland is operating normally.",
                "mr": "टी.एस.एच. सामान्य आहे, थायरॉईडचे कार्य संतुलित सुरू आहे.",
                "hi": "टीएसएच का स्तर सामान्य है, थायराइड ग्रंथि ठीक से काम कर रही है।",
            },
            "high": {
                "en": "High TSH (> 4.5 mIU/L) signals an underactive thyroid (Hypothyroidism). Causes weight gain, hair fall, lethargy, cold intolerance.",
                "mr": "टी.एस.एच. वाढले आहे (> ४.५); थायरॉईडची गती मंदावणे (Hypothyroidism). यामुळे वजन वाढणे, केस गळणे व आळस जाणवू शकतो.",
                "hi": "टीएसएच बढ़ा हुआ है (> 4.5); थायराइड का धीमा काम करना (हाइपोथायरायडिज्म)। इससे वजन बढ़ना, थकान और बाल झड़ना हो सकता है।",
            },
        },
        "dietary_advice": {
            "en": "Use iodized salt; include selenium/zinc-rich seeds (pumpkin/sunflower); limit excess raw cruciferous vegetables (cabbage, cauliflower).",
            "mr": "आयोडीनयुक्त मीठ वापरा; भोपळ्याच्या व सूर्यफुलाच्या बिया खा; कच्ची कोबी व फ्लॉवर जास्त खाणे टाळा.",
            "hi": "आयोडीन युक्त नमक का उपयोग करें; कद्दू के बीज लें; कच्ची गोभी और पत्तागोभी का अधिक सेवन सीमित करें।",
        },
        "doctor_question": {
            "en": "Should we test Free T3 and Free T4, and do I need daily Thyroxine (e.g. Eltroxin/Thyronorm)?",
            "mr": "मला थायरॉईडच्या गोळ्या (उदा. थायरॉनॉर्म) सुरू कराव्या लागतील का, आणि Free T3/T4 टेस्ट करावी का?",
            "hi": "क्या मुझे थायराइड की नियमित दवा शुरू करने और Free T3/T4 टेस्ट कराने की जरूरत है?",
        },
    },
}


# =============================================================================
# 2. ROBUST BIOMARKER EXTRACTION
# =============================================================================

def extract_biomarkers_from_text(raw_text: str) -> Dict[str, Dict[str, Any]]:
    """
    Parses lab report text (from OCR or direct paste) to identify biomarkers,
    observed numeric values, and units.
    Handles commas, decimals, and Indian lakh notations (e.g. '2.1 Lakhs').
    """
    if not raw_text or not isinstance(raw_text, str):
        return {}

    lines = raw_text.splitlines()
    extracted: Dict[str, Dict[str, Any]] = {}

    for line in lines:
        clean = line.strip()
        if len(clean) < 3:
            continue
        clean_low = clean.lower()

        for key, info in BIOMARKER_DATABASE.items():
            if key in extracted:
                continue

            for alias in info["aliases"]:
                # Match alias at word boundary
                pattern = r"(?<!\w)" + re.escape(alias) + r"(?!\w)"
                match_alias = re.search(pattern, clean_low)
                if not match_alias:
                    continue

                remainder = clean[match_alias.end():]

                # 1. Check for Indian lakh format (e.g. 2.1 Lakhs, 1.8 lakh)
                lakh_match = re.search(r"[:=-]?\s*([0-9]+(?:\.[0-9]+)?)\s*(?:lakhs?|lac|लख)", remainder, re.IGNORECASE)
                if lakh_match:
                    try:
                        val_num = round(float(lakh_match.group(1)) * 100000.0, 2)
                        extracted[key] = {
                            "name": info["canonical_name"],
                            "value": val_num,
                            "unit": info["default_unit"],
                            "raw_text": clean,
                        }
                        break
                    except ValueError:
                        pass

                # 2. Check for standard numeric values (with optional decimal or comma)
                val_match = re.search(r"[:=-]?\s*([0-9]+(?:[.,][0-9]+)?)", remainder)
                if not val_match:
                    # Search anywhere in remainder
                    val_match = re.search(r"\b([0-9]+(?:[.,][0-9]+)?)\b", remainder)

                if val_match:
                    raw_num = val_match.group(1).replace(",", "")
                    try:
                        obs_val = round(float(raw_num), 4)
                    except ValueError:
                        continue

                    # Look for explicit unit after value
                    unit_remainder = remainder[val_match.end():].strip()
                    unit_match = re.search(r"^([a-zA-Z/%μ/]+(?:/[a-zA-Z0-9]+)?)", unit_remainder)
                    obs_unit = unit_match.group(1) if unit_match else info["default_unit"]

                    extracted[key] = {
                        "name": info["canonical_name"],
                        "value": obs_val,
                        "unit": obs_unit,
                        "raw_text": clean,
                    }
                    break

    return extracted


# =============================================================================
# 3. CLINICAL STATUS CLASSIFICATION & GAUGE METRICS
# =============================================================================

def calculate_gauge_percent(value: float, ref_min: float, ref_max: float) -> float:
    """
    Normalizes observed biomarker value into a 0.0 - 1.0 gauge fraction:
    - Below ref_min: maps to 0.05 - 0.35
    - Within ref_min to ref_max: maps to 0.35 - 0.65 (midpoint 0.50)
    - Above ref_max: maps to 0.65 - 0.95
    Clamped strictly between 0.0 and 1.0.
    """
    if ref_max <= ref_min:
        return 0.5

    if value < ref_min:
        fraction = max(0.0, value / ref_min) if ref_min > 0 else 0.0
        pct = 0.05 + 0.30 * fraction
    elif value <= ref_max:
        fraction = (value - ref_min) / (ref_max - ref_min)
        pct = 0.35 + 0.30 * fraction
    else:
        overshoot = min(1.0, (value - ref_max) / (ref_max * 0.8))
        pct = 0.65 + 0.30 * overshoot

    return round(max(0.0, min(1.0, pct)), 2)


def classify_biomarker_status(
    value: float,
    ref_min: float,
    ref_max: float,
    crit_low: Optional[float] = None,
    crit_high: Optional[float] = None
) -> Tuple[str, str]:
    """
    Determines status and UI severity color:
    - 'critical_low' -> #F44336 (Red)
    - 'low' -> #FF9800 (Orange)
    - 'normal' -> #4CAF50 (Green)
    - 'high' -> #FF9800 (Orange)
    - 'critical_high' -> #F44336 (Red)
    """
    if crit_low is not None and value <= crit_low:
        return "critical_low", "#F44336"
    if crit_high is not None and value >= crit_high:
        return "critical_high", "#F44336"

    if value < ref_min:
        return "low", "#FF9800"
    elif value > ref_max:
        return "high", "#FF9800"
    return "normal", "#4CAF50"


# =============================================================================
# 4. REPORT ANALYSIS & SUMMARY GENERATION
# =============================================================================

def analyze_lab_report(
    input_data: Union[str, Dict[str, Any]],
    user_language: str = "en"
) -> Dict[str, Any]:
    """
    Analyzes lab report biomarkers and decodes them into layman explanations,
    speedometer gauge percentages, doctor consultation questions, and dietary advice.
    """
    lang = (user_language or "en").strip().lower()
    if lang not in ("mr", "roman_mr", "hi", "hinglish"):
        lang_key = "en"
    elif lang in ("mr", "roman_mr"):
        lang_key = "mr"
    else:
        lang_key = "hi"

    # Step 1: Extract or validate biomarkers
    if isinstance(input_data, str):
        extracted = extract_biomarkers_from_text(input_data)
    elif isinstance(input_data, dict):
        extracted = {}
        for k, v in input_data.items():
            clean_k = k.lower().replace(" ", "_")
            if clean_k in BIOMARKER_DATABASE and isinstance(v, (int, float, dict)):
                val = v.get("value") if isinstance(v, dict) else v
                unit = v.get("unit") if isinstance(v, dict) else BIOMARKER_DATABASE[clean_k]["default_unit"]
                extracted[clean_k] = {
                    "name": BIOMARKER_DATABASE[clean_k]["canonical_name"],
                    "value": float(val),
                    "unit": unit
                }
    else:
        extracted = {}

    analyzed_list = []
    flagged_list = []
    doctor_questions = []

    for key, item in extracted.items():
        if key not in BIOMARKER_DATABASE:
            continue

        info = BIOMARKER_DATABASE[key]
        val = float(item["value"])
        unit = item.get("unit") or info["default_unit"]

        ref_min = info["ref_min"]
        ref_max = info["ref_max"]
        crit_low = info.get("critical_low")
        crit_high = info.get("critical_high")

        status, color = classify_biomarker_status(val, ref_min, ref_max, crit_low, crit_high)
        gauge_pct = calculate_gauge_percent(val, ref_min, ref_max)

        display_name = info["display_name"].get(lang_key, info["canonical_name"])
        what_it_is = info["what_it_is"].get(lang_key, info["what_it_is"]["en"])

        # Determine explanation based on low/normal/high
        base_status = "low" if "low" in status else ("high" if "high" in status else "normal")
        explanation = info["explanations"][base_status].get(lang_key, info["explanations"][base_status]["en"])
        diet_adv = info["dietary_advice"].get(lang_key, info["dietary_advice"]["en"])

        entry = {
            "key": key,
            "name": info["canonical_name"],
            "display_name": display_name,
            "value": val,
            "unit": unit,
            "reference_range": info["ref_str"],
            "status": status,
            "gauge_percent": gauge_pct,
            "severity_color": color,
            "what_it_is": what_it_is,
            "layman_explanation": explanation,
            "dietary_advice": diet_adv,
        }

        analyzed_list.append(entry)

        if status != "normal":
            flagged_list.append(entry)
            doc_q = info["doctor_question"].get(lang_key, info["doctor_question"]["en"])
            if doc_q and doc_q not in doctor_questions:
                doctor_questions.append(doc_q)

    # Cap doctor questions to 3 targeted ones
    if len(doctor_questions) > 3:
        doctor_questions = doctor_questions[:3]
    elif len(doctor_questions) == 0 and len(analyzed_list) > 0:
        # Default healthy check questions
        if lang_key == "mr":
            doctor_questions = [
                "माझे सर्व रिपोर्ट सध्या सामान्य आहेत, भविष्यात हे नियंत्रणात ठेवण्यासाठी आहाराचे काय नियोजन करावे?",
                "मला पुढील नियमित आरोग्य तपासणी कधी करावी लागेल?"
            ]
        elif lang_key == "hi":
            doctor_questions = [
                "मेरी सभी रिपोर्ट सामान्य हैं, भविष्य में इन्हें नियंत्रित रखने के लिए आहार कैसा होना चाहिए?",
                "मुझे अगली नियमित स्वास्थ्य जांच कब करानी चाहिए?"
            ]
        else:
            doctor_questions = [
                "All parameters are currently normal. What routine lifestyle measures should I maintain?",
                "When should I schedule my next routine preventive health checkup?"
            ]

    # Overall Summary Generation
    analyzed_count = len(analyzed_list)
    flagged_count = len(flagged_list)

    if analyzed_count == 0:
        if lang_key == "mr":
            summary = "दिलेल्या माहितीतून कोणतेही प्रमाणित लॅब पॅरामीटर्स (उदा. हिमोग्लोबिन, साखर, क्रिएटिनिन) ओळखता आले नाहीत. कृपया रिपोर्टचा स्पष्ट मजकूर टाका."
        elif lang_key == "hi":
            summary = "प्रदान किए गए पाठ से कोई मानक लैब पैरामीटर (जैसे हीमोग्लोबिन, शुगर, क्रिएटिनिन) नहीं मिल सका। कृपया स्पष्ट रिपोर्ट पेस्ट करें।"
        else:
            summary = "No standard laboratory biomarkers (e.g. Hemoglobin, Glucose, Creatinine) could be detected in the provided text. Please provide clear report text."
    elif flagged_count == 0:
        if lang_key == "mr":
            summary = f"अभिनंदन! तपासण्यात आलेले सर्व {analyzed_count} घटक सामान्य मर्यादेत आहेत. कोणतीही गंभीर किंवा चिंताजनक बाब आढळलेली नाही."
        elif lang_key == "hi":
            summary = f"बधाई! जांचे गए सभी {analyzed_count} पैरामीटर सामान्य सीमा में हैं। कोई भी चिंताजनक रिपोर्ट नहीं पाई गई।"
        else:
            summary = f"All {analyzed_count} evaluated biomarkers are currently within standard healthy reference ranges. No abnormal parameters detected."
    else:
        flagged_names = [f["display_name"] for f in flagged_list]
        names_str = ", ".join(flagged_names)
        if lang_key == "mr":
            summary = f"तपासण्यात आलेल्या {analyzed_count} घटकांपैकी {flagged_count} घटकांमध्ये तफावत आढळली आहे ({names_str}). कृपया खालील सविस्तर विश्लेषण आणि डॉक्टरांना विचारण्यासाठीचे प्रश्न तपासा."
        elif lang_key == "hi":
            summary = f"जांचे गए {analyzed_count} घटकों में से {flagged_count} घटक सामान्य सीमा से बाहर पाए गए हैं ({names_str})। कृपया नीचे दिया गया विश्लेषण और डॉक्टर से पूछने योग्य प्रश्न देखें।"
        else:
            summary = f"Out of {analyzed_count} analyzed biomarkers, {flagged_count} out-of-range parameter(s) were flagged: {names_str}. Review the detailed insights and suggested physician questions below."

    # Standard Disclaimer
    if lang_key == "mr":
        disclaimer = "⚠️ **महत्त्वाची सूचना:** हा अहवाल केवळ समजण्यासाठी व शैक्षणिक मार्गदर्शनासाठी आहे. हे अंतिम वैद्यकीय निदान नाही. कृपया कोणताही औषधोपचार सुरू किंवा बंद करण्यापूर्वी आपल्या नोंदणीकृत डॉक्टरांचा सल्ला नक्की घ्या."
    elif lang_key == "hi":
        disclaimer = "⚠️ **महत्वपूर्ण सूचना:** यह विश्लेषण केवल आपकी समझ और शैक्षिक मार्गदर्शन के लिए है। यह कोई अंतिम डॉक्टरी निदान नहीं है। किसी भी दवा के लिए कृपया अपने चिकित्सक से परामर्श लें।"
    else:
        disclaimer = "⚠️ **Clinical Disclaimer:** This automated decoder is for patient educational guidance only. A lab test is never a definitive diagnosis. Always review these results with your treating physician before initiating or modifying any medical treatment."

    return {
        "status": "success",
        "language": lang_key,
        "analyzed_count": analyzed_count,
        "flagged_count": flagged_count,
        "biomarkers": analyzed_list,
        "doctor_questions": doctor_questions,
        "summary": summary,
        "disclaimer": disclaimer,
    }
