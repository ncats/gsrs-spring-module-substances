<?xml version="1.0" encoding="UTF-8"?>
<!--
    EMA SPOR referential (RMS) list data shared by ``import-ema-fhir-json.xsl``
    and ``export-ema-fhir-json.xsl``, so a term is recorded once.

    Every map is keyed by term id and ordered by ascending id, which keeps this
    file diffable against a fresh export of the same lists. Every entry has the
    same shape, ``map { 'value': ..., 'label': ... }``: ``value`` is the GSRS
    side of the mapping and ``label`` the list's own display name. A list GSRS
    has no field for, such as the domain a substance is in, leaves ``value``
    out and carries the label alone. The import reads ``value`` and the export
    goes the other way through ``ema:index``.

    The list ids and the terms a list falls back on are named here as well, so
    a stylesheet that writes a coded value reaches the list through
    ``ema:list-system`` instead of repeating the SPOR url.

    The data was transcribed from ``rms_lists.csv``, a build time source only:
    the stylesheets have no file access and the CSV is not shipped in the wheel.
-->
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:ema="urn:gsrs:ema-rms"
                exclude-result-prefixes="ema fn xs map">

    <!-- The SPOR url a list id is written under. Every coded value this
         vocabulary is written as is a ``system`` plus a term id, and the
         system is the same url with the list id appended, so the id alone is
         enough to reach it. -->
    <xsl:function name="ema:list-system" as="xs:string">
        <xsl:param name="list" as="xs:string"/>
        <xsl:sequence select="concat('https://spor.ema.europa.eu/v1/lists/', $list)"/>
    </xsl:function>

    <!-- The lists, by id, so a stylesheet names which one it writes rather
         than spelling the id out at each call site. -->
    <xsl:variable name="ema-domain-list" as="xs:string" select="'100000000004'"/>
    <xsl:variable name="ema-code-system-list" as="xs:string" select="'100000000009'"/>
    <xsl:variable name="ema-language-list" as="xs:string" select="'100000072057'"/>
    <xsl:variable name="ema-category-list" as="xs:string" select="'100000075826'"/>
    <xsl:variable name="ema-data-classification-list" as="xs:string" select="'200000004983'"/>
    <xsl:variable name="ema-status-list" as="xs:string" select="'200000005003'"/>

    <!-- List 100000000004: the domain a substance is in.

         Nothing in GSRS corresponds to it, so no term carries a ``value``: the
         domain is written from the label alone.
    -->
    <xsl:variable name="ema-domain-terms" as="map(*)" select="map {
        '100000000012': map { 'label': 'Human use' },
        '100000000013': map { 'label': 'Veterinary use' },
        '100000000014': map { 'label': 'Human and Veterinary use' }
    }"/>

    <!-- The domain the EMA lists describe. The payload has no field to say it,
         so the export writes this term on every substance rather than leaving
         the domain out. -->
    <xsl:variable name="ema-domain-human-use" as="xs:string" select="'100000000012'"/>

    <!-- List 100000075826: substance category.

         ``value`` is the GSRS substanceClass the export writes, ``label`` the
         list's own display name.
    -->
    <xsl:variable name="ema-category-terms" as="map(*)" select="map {
        '100000075670': map { 'value': 'chemical', 'label': 'Chemical' },
        '200000005020': map { 'value': 'protein', 'label': 'Protein' },
        '200000005022': map { 'value': 'polymer', 'label': 'Polymer' },
        '200000005023': map { 'value': 'mixture', 'label': 'Mixture' },
        '200000005024': map {
            'value': 'structurally diverse - plasma derived',
            'label': 'Structurally Diverse - Plasma derived'
        },
        '200000005025': map {
            'value': 'structurally diverse - herbal',
            'label': 'Structurally Diverse - Herbal'
        },
        '200000005026': map {
            'value': 'structurally diverse - allergen',
            'label': 'Structurally Diverse - Allergen'
        },
        '200000005027': map {
            'value': 'structurally diverse - vaccine',
            'label': 'Structurally Diverse - Vaccine'
        },
        '200000005029': map {
            'value': 'structurally diverse - cell therapy',
            'label': 'Structurally Diverse - Cell therapy'
        },
        '200000005030': map {
            'value': 'structurallyDiverse',
            'label': 'Structurally Diverse - Other'
        },
        '200000005031': map {
            'value': 'specifiedSubstanceG1',
            'label': 'Specified Substance Group 1'
        },
        '200000005032': map {
            'value': 'specifiedSubstanceG2',
            'label': 'Specified Substance Group 2'
        },
        '200000005033': map {
            'value': 'specifiedSubstanceG3',
            'label': 'Specified Substance Group 3'
        },
        '200000005034': map {
            'value': 'specifiedSubstanceG4',
            'label': 'Specified Substance Group 4'
        },
        '200000005035': map { 'value': 'nucleicAcid', 'label': 'Nucleic acid' }
    }"/>

    <!-- The category a substance class the list does not name falls back on,
         which is the one GSRS calls chemical. -->
    <xsl:variable name="ema-category-default" as="xs:string" select="'100000075670'"/>

    <!-- List 200000004983: data classification, the access the EMA profile
         puts on an element. GSRS ``access`` is either empty or not, so only
         two of the terms are ever written and both are named here rather than
         looked up by label. -->
    <xsl:variable name="ema-data-classification-terms" as="map(*)" select="map {
        '200000004984': map { 'label': 'Confidential' },
        '200000004985': map { 'label': 'Public' }
    }"/>

    <!-- The two classification terms, by meaning: empty GSRS ``access`` is
         ``Public`` and anything else is ``Confidential``. -->
    <xsl:variable name="ema-data-classification-public" as="xs:string" select="'200000004985'"/>
    <xsl:variable name="ema-data-classification-confidential" as="xs:string" select="'200000004984'"/>

    <!-- List 200000005003: the publication status of an element. Both terms
         are named here rather than looked up by label, because the two sides
         start from different GSRS fields: the import reads the FHIR coding and
         the export reads GSRS ``deprecated``. -->
    <xsl:variable name="ema-status-terms" as="map(*)" select="map {
        '200000005004': map { 'label': 'Current' },
        '200000005006': map { 'label': 'Non-Current' }
    }"/>

    <!-- The two publication terms, by meaning. ``Non-Current`` is the one that
         means deprecated on the GSRS side and ``Current`` the default. -->
    <xsl:variable name="ema-status-current" as="xs:string" select="'200000005004'"/>
    <xsl:variable name="ema-status-non-current" as="xs:string" select="'200000005006'"/>

    <!-- List 100000072057: language.

         ``value`` is the ISO 639-1 code GSRS stores, ``label`` the list's own
         display name.
    -->
    <xsl:variable name="ema-language-terms" as="map(*)" select="map {
        '100000072115': map { 'value': 'ab', 'label': 'Abkhazian' },
        '100000072116': map { 'value': 'sq', 'label': 'Albanian' },
        '100000072117': map { 'value': 'hy', 'label': 'Armenian' },
        '100000072118': map { 'value': 'ae', 'label': 'Avestan' },
        '100000072119': map { 'value': 'ba', 'label': 'Bashkir' },
        '100000072120': map { 'value': 'bn', 'label': 'Bengali' },
        '100000072124': map { 'value': 'aa', 'label': 'Afar' },
        '100000072125': map { 'value': 'af', 'label': 'Afrikaans' },
        '100000072126': map { 'value': 'ak', 'label': 'Akan' },
        '100000072127': map { 'value': 'am', 'label': 'Amharic' },
        '100000072128': map { 'value': 'ar', 'label': 'Arabic' },
        '100000072129': map { 'value': 'an', 'label': 'Aragonese' },
        '100000072130': map { 'value': 'as', 'label': 'Assamese' },
        '100000072131': map { 'value': 'av', 'label': 'Avaric' },
        '100000072132': map { 'value': 'ay', 'label': 'Aymara' },
        '100000072133': map { 'value': 'az', 'label': 'Azerbaijani' },
        '100000072134': map { 'value': 'bm', 'label': 'Bambara' },
        '100000072135': map { 'value': 'eu', 'label': 'Basque' },
        '100000072136': map { 'value': 'be', 'label': 'Belarusian' },
        '100000072137': map { 'value': 'bh', 'label': 'Bihari' },
        '100000072138': map { 'value': 'bi', 'label': 'Bislama' },
        '100000072139': map { 'value': 'br', 'label': 'Breton' },
        '100000072140': map { 'value': 'my', 'label': 'Burmese' },
        '100000072141': map { 'value': 'bs', 'label': 'Bosnian' },
        '100000072142': map { 'value': 'bg', 'label': 'Bulgarian' },
        '100000072143': map { 'value': 'ch', 'label': 'Chamorro' },
        '100000072144': map { 'value': 'cv', 'label': 'Chuvash' },
        '100000072145': map { 'value': 'cr', 'label': 'Cree' },
        '100000072146': map { 'value': 'dv', 'label': 'Maldivian' },
        '100000072147': map { 'value': 'en', 'label': 'English' },
        '100000072148': map { 'value': 'ee', 'label': 'Ewe' },
        '100000072149': map { 'value': 'fi', 'label': 'Finnish' },
        '100000072150': map { 'value': 'ff', 'label': 'Fulah' },
        '100000072151': map { 'value': 'gd', 'label': 'Gaelic' },
        '100000072152': map { 'value': 'gv', 'label': 'Manx' },
        '100000072153': map { 'value': 'gu', 'label': 'Gujarati' },
        '100000072154': map { 'value': 'he', 'label': 'Hebrew' },
        '100000072155': map { 'value': 'ho', 'label': 'Hiri Motu' },
        '100000072156': map { 'value': 'is', 'label': 'Icelandic' },
        '100000072157': map { 'value': 'iu', 'label': 'Inuktitut' },
        '100000072158': map { 'value': 'id', 'label': 'Indonesian' },
        '100000072159': map { 'value': 'jv', 'label': 'Javanese' },
        '100000072160': map { 'value': 'kn', 'label': 'Kannada' },
        '100000072161': map { 'value': 'ca', 'label': 'Catalan' },
        '100000072162': map { 'value': 'ce', 'label': 'Chechen' },
        '100000072163': map { 'value': 'zh', 'label': 'Chinese' },
        '100000072164': map { 'value': 'cu', 'label': 'Church Slavic' },
        '100000072165': map { 'value': 'kw', 'label': 'Cornish' },
        '100000072166': map { 'value': 'co', 'label': 'Corsican' },
        '100000072167': map { 'value': 'cs', 'label': 'Czech' },
        '100000072168': map { 'value': 'da', 'label': 'Danish' },
        '100000072169': map { 'value': 'nl', 'label': 'Dutch' },
        '100000072170': map { 'value': 'dz', 'label': 'Dzongkha' },
        '100000072171': map { 'value': 'eo', 'label': 'Esperanto' },
        '100000072172': map { 'value': 'et', 'label': 'Estonian' },
        '100000072173': map { 'value': 'fo', 'label': 'Faroese' },
        '100000072174': map { 'value': 'fj', 'label': 'Fijian' },
        '100000072175': map { 'value': 'fr', 'label': 'French' },
        '100000072176': map { 'value': 'fy', 'label': 'Western Frisian' },
        '100000072177': map { 'value': 'ka', 'label': 'Georgian' },
        '100000072178': map { 'value': 'de', 'label': 'German' },
        '100000072179': map { 'value': 'ga', 'label': 'Irish' },
        '100000072180': map { 'value': 'gl', 'label': 'Galician' },
        '100000072181': map { 'value': 'el', 'label': 'Greek, Modern (1453-)' },
        '100000072182': map { 'value': 'gn', 'label': 'Guarani' },
        '100000072183': map { 'value': 'ht', 'label': 'Haitian' },
        '100000072184': map { 'value': 'ha', 'label': 'Hausa' },
        '100000072185': map { 'value': 'hz', 'label': 'Herero' },
        '100000072186': map { 'value': 'hi', 'label': 'Hindi' },
        '100000072187': map { 'value': 'hu', 'label': 'Hungarian' },
        '100000072188': map { 'value': 'ig', 'label': 'Igbo' },
        '100000072189': map { 'value': 'io', 'label': 'Ido' },
        '100000072190': map { 'value': 'ii', 'label': 'Sichuan Yi' },
        '100000072191': map { 'value': 'ie', 'label': 'Interlingue' },
        '100000072192': map {
            'value': 'ia',
            'label': 'Interlingua (International Auxiliary Language Association)'
        },
        '100000072193': map { 'value': 'ik', 'label': 'Inupiaq' },
        '100000072194': map { 'value': 'it', 'label': 'Italian' },
        '100000072195': map { 'value': 'ja', 'label': 'Japanese' },
        '100000072196': map { 'value': 'kl', 'label': 'Greenlandic' },
        '100000072197': map { 'value': 'ks', 'label': 'Kashmiri' },
        '100000072198': map { 'value': 'kr', 'label': 'Kanuri' },
        '100000072199': map { 'value': 'km', 'label': 'Central Khmer' },
        '100000072200': map { 'value': 'ki', 'label': 'Kikuyu' },
        '100000072201': map { 'value': 'kk', 'label': 'Kazakh' },
        '100000072202': map { 'value': 'rw', 'label': 'Kinyarwanda' },
        '100000072203': map { 'value': 'kg', 'label': 'Kongo' },
        '100000072204': map { 'value': 'ku', 'label': 'Kurdish' },
        '100000072205': map { 'value': 'lv', 'label': 'Latvian' },
        '100000072206': map { 'value': 'lt', 'label': 'Lithuanian' },
        '100000072207': map { 'value': 'lg', 'label': 'Ganda' },
        '100000072208': map { 'value': 'ml', 'label': 'Malayalam' },
        '100000072209': map { 'value': 'ms', 'label': 'Malay' },
        '100000072210': map { 'value': 'mo', 'label': 'Moldavian' },
        '100000072211': map { 'value': 'nv', 'label': 'Navaho' },
        '100000072212': map { 'value': 'ng', 'label': 'Ndonga' },
        '100000072213': map { 'value': 'nb', 'label': 'Bokmål, Norwegian' },
        '100000072214': map { 'value': 'ny', 'label': 'Chichewa' },
        '100000072215': map { 'value': 'or', 'label': 'Oriya' },
        '100000072216': map { 'value': 'pa', 'label': 'Panjabi' },
        '100000072217': map { 'value': 'pl', 'label': 'Polish' },
        '100000072218': map { 'value': 'qu', 'label': 'Quechua' },
        '100000072219': map { 'value': 'rn', 'label': 'Rundi' },
        '100000072220': map { 'value': 'sa', 'label': 'Sanskrit' },
        '100000072221': map { 'value': 'ky', 'label': 'Kirghiz' },
        '100000072222': map { 'value': 'kv', 'label': 'Komi' },
        '100000072223': map { 'value': 'ko', 'label': 'Korean' },
        '100000072224': map { 'value': 'kj', 'label': 'Kuanyama' },
        '100000072225': map { 'value': 'lo', 'label': 'Lao' },
        '100000072226': map { 'value': 'la', 'label': 'Latin' },
        '100000072227': map { 'value': 'li', 'label': 'Limburgan' },
        '100000072228': map { 'value': 'ln', 'label': 'Lingala' },
        '100000072229': map { 'value': 'lb', 'label': 'Luxembourgish' },
        '100000072230': map { 'value': 'lu', 'label': 'Luba-Katanga' },
        '100000072231': map { 'value': 'mk', 'label': 'Macedonian' },
        '100000072232': map { 'value': 'mh', 'label': 'Marshallese' },
        '100000072233': map { 'value': 'mi', 'label': 'Maori' },
        '100000072234': map { 'value': 'mr', 'label': 'Marathi' },
        '100000072235': map { 'value': 'mg', 'label': 'Malagasy' },
        '100000072236': map { 'value': 'mt', 'label': 'Maltese' },
        '100000072237': map { 'value': 'mn', 'label': 'Mongolian' },
        '100000072238': map { 'value': 'na', 'label': 'Nauru' },
        '100000072239': map { 'value': 'nr', 'label': 'South Ndebele' },
        '100000072240': map { 'value': 'nd', 'label': 'North Ndebele' },
        '100000072241': map { 'value': 'ne', 'label': 'Nepali' },
        '100000072242': map { 'value': 'nn', 'label': 'Norwegian Nynorsk' },
        '100000072243': map { 'value': 'no', 'label': 'Norwegian' },
        '100000072245': map { 'value': 'oc', 'label': 'Occitan (post 1500)' },
        '100000072246': map { 'value': 'oj', 'label': 'Ojibwa' },
        '100000072247': map { 'value': 'om', 'label': 'Oromo' },
        '100000072248': map { 'value': 'os', 'label': 'Ossetian' },
        '100000072249': map { 'value': 'fa', 'label': 'Persian' },
        '100000072250': map { 'value': 'pi', 'label': 'Pali' },
        '100000072251': map { 'value': 'pt', 'label': 'Portuguese' },
        '100000072252': map { 'value': 'ps', 'label': 'Pushto' },
        '100000072253': map { 'value': 'rm', 'label': 'Romansh' },
        '100000072254': map { 'value': 'ro', 'label': 'Romanian' },
        '100000072255': map { 'value': 'ru', 'label': 'Russian' },
        '100000072256': map { 'value': 'sg', 'label': 'Sango' },
        '100000072257': map { 'value': 'sr', 'label': 'Serbian' },
        '100000072258': map { 'value': 'hr', 'label': 'Croatian' },
        '100000072259': map { 'value': 'sk', 'label': 'Slovak' },
        '100000072260': map { 'value': 'sl', 'label': 'Slovenian' },
        '100000072261': map { 'value': 'si', 'label': 'Sinhala' },
        '100000072262': map { 'value': 'se', 'label': 'Northern Sami' },
        '100000072263': map { 'value': 'sd', 'label': 'Sindhi' },
        '100000072264': map { 'value': 'es', 'label': 'Spanish' },
        '100000072265': map { 'value': 'su', 'label': 'Sundanese' },
        '100000072266': map { 'value': 'ty', 'label': 'Tahitian' },
        '100000072267': map { 'value': 'te', 'label': 'Telugu' },
        '100000072268': map { 'value': 'th', 'label': 'Thai' },
        '100000072269': map { 'value': 'to', 'label': 'Tonga (Tonga Islands)' },
        '100000072270': map { 'value': 'tk', 'label': 'Turkmen' },
        '100000072271': map { 'value': 'ug', 'label': 'Uighur' },
        '100000072272': map { 'value': 'uz', 'label': 'Uzbek' },
        '100000072273': map { 'value': 'wa', 'label': 'Walloon' },
        '100000072274': map { 'value': 'yo', 'label': 'Yoruba' },
        '100000072281': map { 'value': 'sm', 'label': 'Samoan' },
        '100000072282': map { 'value': 'sn', 'label': 'Shona' },
        '100000072283': map { 'value': 'so', 'label': 'Somali' },
        '100000072284': map { 'value': 'st', 'label': 'Sotho, Southern' },
        '100000072285': map { 'value': 'sc', 'label': 'Sardinian' },
        '100000072286': map { 'value': 'ss', 'label': 'Swati' },
        '100000072287': map { 'value': 'sw', 'label': 'Swahili' },
        '100000072288': map { 'value': 'sv', 'label': 'Swedish' },
        '100000072289': map { 'value': 'ta', 'label': 'Tamil' },
        '100000072290': map { 'value': 'tt', 'label': 'Tatar' },
        '100000072291': map { 'value': 'tg', 'label': 'Tajik' },
        '100000072292': map { 'value': 'tl', 'label': 'Tagalog' },
        '100000072293': map { 'value': 'bo', 'label': 'Tibetan' },
        '100000072294': map { 'value': 'ti', 'label': 'Tigrinya' },
        '100000072295': map { 'value': 'tn', 'label': 'Tswana' },
        '100000072296': map { 'value': 'ts', 'label': 'Tsonga' },
        '100000072297': map { 'value': 'tr', 'label': 'Turkish' },
        '100000072298': map { 'value': 'tw', 'label': 'Twi' },
        '100000072299': map { 'value': 'uk', 'label': 'Ukrainian' },
        '100000072300': map { 'value': 'ur', 'label': 'Urdu' },
        '100000072301': map { 'value': 've', 'label': 'Venda' },
        '100000072302': map { 'value': 'vi', 'label': 'Vietnamese' },
        '100000072303': map { 'value': 'vo', 'label': 'Volapük' },
        '100000072304': map { 'value': 'cy', 'label': 'Welsh' },
        '100000072305': map { 'value': 'wo', 'label': 'Wolof' },
        '100000072306': map { 'value': 'xh', 'label': 'Xhosa' },
        '100000072307': map { 'value': 'yi', 'label': 'Yiddish' },
        '100000072308': map { 'value': 'za', 'label': 'Zhuang' },
        '100000072309': map { 'value': 'zu', 'label': 'Zulu' }
    }"/>

    <!-- The language a code the list does not name falls back on, which is the
         one GSRS calls en. -->
    <xsl:variable name="ema-language-default" as="xs:string" select="'100000072147'"/>

    <!-- List 100000000009: code system.

         One list, read two ways. A GSRS code carries a ``codeSystem`` name and
         a FHIR ``name.source`` carries a term id, and both point at these
         terms, so a single table answers either direction: the import writes
         a code's ``codeSystem`` from ``value`` and a citation from ``label``,
         and the export goes back the other way through ``ema:index``.

         ``value`` is the GSRS codeSystem name, and only the terms GSRS has one
         for carry it: a citation needs nothing but the label, so most entries
         here have none. ECHA is the one GSRS name with two terms behind it,
         100000170925 in ``rms_lists.csv`` and 200000032418, the one the export
         writes for a code. The two directions therefore land on different ids,
         and each lands on the right one: a code is written with the id the EMA
         documents carry, while a citation quotes a label, and only
         100000170925 has the label, so only it can answer one. A source reads
         back as the label of the term it names and exports again as the term
         that carries that label, which is how a document round trips.
    -->
    <xsl:variable name="ema-code-system-terms" as="map(*)" select="map {
        '100000000015': map { 'label': 'World Heath Organization' },
        '100000000016': map { 'label': 'European Directorate for the Quality of Medicines &amp; HealthCare' },
        '100000000017': map { 'label': 'International Conference of Harmonisation of Technical Requirements for Registration of Pharmaceuticals for Human Use' },
        '100000000018': map { 'label': 'Maintenance and Support Services Organization' },
        '100000000019': map { 'label': 'Medical Dictionary for Regulatory Activities' },
        '100000000020': map { 'label': 'Committee for Medicinal Products for Veterinary Use' },
        '100000000021': map { 'label': 'Veterinary Dictionary For Drug Regulatory Activities' },
        '100000000022': map { 'label': 'International Organization for Standardization' },
        '100000000023': map { 'label': '3-digit ISO 3166-1 Codes for the representation of names of countries and their subdivisions' },
        '100000000024': map { 'label': 'Representation of human sexes' },
        '100000072041': map { 'label': '2-letter ISO 3166-1 Codes for the representation of names of countries and their subdivisions' },
        '100000072042': map { 'label': '3-letter ISO 3166-1 Codes for the representation of names of countries and their subdivisions' },
        '100000072043': map { 'label': 'World Health Organization-Adverse Reaction Terminology' },
        '100000072044': map { 'label': 'Hoechst Adverse Reaction Terminology' },
        '100000072045': map { 'label': 'Coding Symbols for a Thesaurus of Adverse Reaction Terms.' },
        '100000072046': map { 'label': 'International Statistical Classification of Diseases and Related Health Problems' },
        '100000072047': map { 'label': 'Japanese Adverse Reaction Terminology' },
        '100000072048': map { 'label': 'ISO 639-1 Codes for the representation of names of languages' },
        '100000073351': map { 'label': 'European Pharmacopoeia' },
        '100000073361': map { 'label': 'Standard Terms for dosage forms, routes of administration and containers' },
        '100000075665': map { 'value': 'EVMPD', 'label': 'Extended EudraVigilance Medicinal Product Dictionary' },
        '100000075674': map { 'label': 'UPDATE' },
        '100000075675': map { 'label': 'MONOGRAPHY 1928' },
        '100000075676': map { 'label': 'PHARMACOPOEA BOHEMICA MCMXCVII' },
        '100000075677': map { 'label': 'GENOMNET' },
        '100000075678': map { 'label': 'RINNM' },
        '100000075679': map { 'label': 'PINNM' },
        '100000075680': map { 'label': 'EVMPD' },
        '100000075681': map { 'label': 'NATIONAL LIBRARY OF MEDICINE' },
        '100000075682': map { 'label': 'FAO FOOD AND NUTRITION MEETINGS REPORT' },
        '100000075683': map { 'label': 'FAO FOOD &amp; NUTRITION MEETING REPORT' },
        '100000075684': map { 'label': 'ALK-ABELLO' },
        '100000075685': map { 'label': 'HANDBOOK OF PHARMACEUTICAL EXCIPIENTS 4TH ED' },
        '100000075686': map { 'label': 'VIDAL' },
        '100000075687': map { 'label': 'CABRI-COMMON ACCESS TO BIOLOGICAL RESOURCES AND INFORMATION' },
        '100000075688': map { 'label': 'JAN' },
        '100000075689': map { 'label': 'MARTINDALE 34TH EDITION' },
        '100000075690': map { 'label': 'JAPANESE PHARMACOPOEIA' },
        '100000075691': map { 'label': 'USUAL COMMON NAME' },
        '100000075692': map { 'label': 'HANDBOOK OF PHARMACEUTICAL EXCIPIENT (2ND EDITION)' },
        '100000075693': map { 'label': 'MARTINDALE 34 EDITION' },
        '100000075694': map { 'label': 'POLPHARMA SPECIFICATION' },
        '100000075695': map { 'label': 'UCB SPECIFICATIONS' },
        '100000075696': map { 'label': 'SMPC' },
        '100000075697': map { 'label': 'DEUTSCHES ARZNEIBUCH' },
        '100000075698': map { 'label': 'MANUFACTURE''S NAME' },
        '100000075699': map { 'label': 'INNM' },
        '100000075700': map { 'label': 'BANM' },
        '100000075701': map { 'label': 'JPN' },
        '100000075702': map { 'label': 'FR.P.' },
        '100000075703': map { 'label': 'HANDBOOK OF PHARMACEUTICAL EXCIPIENTS (SECOND EDITION)' },
        '100000075704': map { 'label': 'CHEMICAL' },
        '100000075705': map { 'label': 'MARTINDALE 29TH ED.' },
        '100000075706': map { 'label': 'THE UNITED STATES PHARMACOPOEIA 24TH ED.' },
        '100000075707': map { 'label': 'MANUFACTURER' },
        '100000075708': map { 'label': 'EUROPEAN ECONOMIC COMMUNITY' },
        '100000075709': map { 'label': 'COMPANY DEVELOPMENT CODE' },
        '100000075710': map { 'label': 'ELC' },
        '100000075711': map { 'label': 'JP PHARMACOPOEIA' },
        '100000075712': map { 'label': 'COMPANY SPECIFICATION' },
        '100000075713': map { 'label': 'SUMMARY OF PRODUCT CHARACTERISTICS' },
        '100000075714': map { 'label': 'THE MERCK INDEX 13TH EDITION' },
        '100000075715': map { 'value': 'INN', 'label': 'INN' },
        '100000075716': map { 'label': 'EU PHARMACOPOEIA' },
        '100000075717': map { 'label': 'THE MERCK INDEX 20TH EDITION' },
        '100000075718': map { 'label': 'EUROPEAN PHARMACOPOEIA SUPPLEMENT 4.2' },
        '100000075719': map { 'label': 'MARTINDALE 33 EDITION' },
        '100000075720': map { 'label': 'THE MERCK INDEX 12TH EDITION' },
        '100000075721': map { 'label': 'NATIONAL FORMULARY' },
        '100000075722': map { 'label': 'NO OFFICIAL MONOGRAPH' },
        '100000075723': map { 'label': 'BRITISH PHARMACOPOEIA' },
        '100000075724': map { 'label': 'AVANTI POLAR LIPIDS CATALOGUE' },
        '100000075725': map { 'label': 'OTHER' },
        '100000075726': map { 'label': 'WHO' },
        '100000075727': map { 'label': 'OSPECIFICERAD' },
        '100000075728': map { 'label': 'COMP' },
        '100000075729': map { 'label': '76 16' },
        '100000075730': map { 'label': '82 19' },
        '100000075731': map { 'label': 'EPM' },
        '100000075732': map { 'label': 'IPH' },
        '100000075733': map { 'label': 'ONF' },
        '100000075734': map { 'label': 'BAN' },
        '100000075735': map { 'label': 'HBEXIP' },
        '100000075736': map { 'label': 'USAN' },
        '100000075737': map { 'label': 'SWED' },
        '100000075738': map { 'label': 'USP' },
        '100000075739': map { 'label': 'BP' },
        '100000075740': map { 'label': '78 17' },
        '100000075741': map { 'label': 'SCHERING' },
        '100000075742': map { 'label': 'SPC' },
        '100000075743': map { 'label': 'MONOGRAPH' },
        '100000075744': map { 'label': 'USP/USNF' },
        '100000075745': map { 'label': 'FRENCH PHARMACOPOEIA' },
        '100000075746': map { 'label': 'FU' },
        '100000075747': map { 'label': 'SUPPLIER DOCUMENTATION' },
        '100000075748': map { 'label': 'ITALIAN PHARMACOPOEIA' },
        '100000075749': map { 'label': 'SUPPLIER MONOGRAPH' },
        '100000075750': map { 'label': 'OTHER SOURCES' },
        '100000075751': map { 'label': 'HSE' },
        '100000075752': map { 'label': 'USNF' },
        '100000075753': map { 'label': 'USP/NF' },
        '100000075754': map { 'label': 'DAB' },
        '100000075755': map { 'label': 'NF/PH. FRANC./95/45EEC' },
        '100000075756': map { 'label': 'SCHERING ITALY' },
        '100000075757': map { 'label': 'BAN/USAN' },
        '100000075758': map { 'label': 'DAB/NF' },
        '100000075759': map { 'label': 'NF/78/25EEC' },
        '100000075760': map { 'label': 'NF' },
        '100000075761': map { 'label': 'AI' },
        '100000075762': map { 'label': 'IPSEN' },
        '100000075763': map { 'label': 'INTERNAL STANDARD' },
        '100000075764': map { 'label': 'MARTINDALE 35TH' },
        '100000075765': map { 'label': 'BIOLOGICAL' },
        '100000075766': map { 'label': 'CHEMINDUSTRYWEB' },
        '100000075767': map { 'label': 'INCI' },
        '100000075768': map { 'label': 'NF 18' },
        '100000075769': map { 'label': 'USP 23' },
        '100000075770': map { 'label': 'INTERNATIONAL NON-PROPRIETARY NAME' },
        '100000075771': map { 'label': 'MANUFACTURER''S MONOGRAPH' },
        '100000075772': map { 'label': 'FOOD COLOUR EEC DIRECTIVE' },
        '100000075773': map { 'label': 'FCC' },
        '100000075774': map { 'label': 'FAO FOOD &amp; NUTRITION MEETINGS REPORT' },
        '100000075775': map { 'label': 'BRITISH NATIONAL FORMULARY' },
        '100000075776': map { 'label': 'SACCHAROMYCES CEREVISIAE' },
        '100000075777': map { 'label': 'MARTINDALE 33 ED.' },
        '100000075778': map { 'label': 'NCBI-PUBCHEM WEBSITE' },
        '100000075779': map { 'label': 'CUSTOM GROUP' },
        '100000075780': map { 'label': 'SYNKEM' },
        '100000075781': map { 'label': 'COPRIMA' },
        '100000075782': map { 'label': 'EPROVA' },
        '100000075783': map { 'label': 'BIAM' },
        '100000075784': map { 'label': 'ROTELISTE' },
        '100000075785': map { 'label': 'PACKAGE INSERT USA' },
        '100000075786': map { 'label': 'INTERNATIONAL NOMENCLATURE COSMETIC INGREDIENT (INCI)' },
        '100000075787': map { 'value': 'CAS', 'label': 'CHEMICAL ABSTRACT SERVICE' },
        '100000075788': map { 'label': 'ASK' },
        '100000075789': map { 'label': 'INVESTIGATOR BROCHURE' },
        '100000075790': map { 'label': 'EUROPEAN PHARMACOPOEIA' },
        '100000075791': map { 'label': 'ALK-ABELLO A/S' },
        '100000075792': map { 'label': 'PH.EUR' },
        '100000075793': map { 'label': 'USP 29' },
        '100000075794': map { 'label': 'PHARMACOPOEA BOHEMOSLOVACA IV' },
        '100000075795': map { 'label': 'ATC/WHO' },
        '100000075796': map { 'label': 'HANDBOOK OF PHARMACEUTICAL EXCIPIENTS 5TH ED' },
        '100000075797': map { 'label': 'POLISH PHARMACOPOEIA' },
        '100000075798': map { 'label': 'RINN' },
        '100000075799': map { 'label': 'USP, EP' },
        '100000075800': map { 'label': 'PH.FR' },
        '100000075801': map { 'label': 'IUPAC' },
        '100000075802': map { 'label': 'KEW GARDEN (WORLD CHECKLIST)' },
        '100000075803': map { 'label': 'KEW GARDEN (IPLANTS DATABASE)' },
        '100000075804': map { 'label': 'EINECS' },
        '100000075805': map { 'label': 'COMPANY PROPRIETARY DATA' },
        '100000075806': map { 'label': 'ÖAB' },
        '100000075807': map { 'label': 'INVESTIGATOR''S BROCHURE' },
        '100000075808': map { 'label': 'BRITISH PHARMACOPEIA' },
        '100000075809': map { 'label': 'JP' },
        '100000075810': map { 'label': 'USP, PH.EUR' },
        '100000075811': map { 'label': 'N/A' },
        '100000075812': map { 'label': 'USP, EP, JP' },
        '100000075813': map { 'label': 'MARTINDALE 31ST EDITION' },
        '100000075814': map { 'label': 'NEUROCRINE BIOSCIENCES INC' },
        '100000075815': map { 'label': 'INTERNATIONAL PLANT NAMES INDEX' },
        '100000075816': map { 'label': 'CLINICAL TRIAL' },
        '100000075817': map { 'label': 'HOFFMANN LAROCHE' },
        '100000075818': map { 'label': 'GERMAN COMMISSION E MONOGRAPHS' },
        '100000075819': map { 'label': 'JAPANESE PHARMACEUTICAL CODEX' },
        '100000075820': map { 'label': 'GLOBAL DIVERSITY INFORMATION FACILITY' },
        '100000075821': map { 'label': 'POLISH PHARMACOPOEA VI EDITION' },
        '100000075822': map { 'label': 'USP PHARMACOPEIA' },
        '100000075823': map { 'label': 'IN-HOUSE MONOGRAPH' },
        '100000075824': map { 'label': 'INFLUENZA RECOMMENDATIONS' },
        '100000093553': map { 'label': 'Anatomical Therapeutic Chemical Classification System - Human' },
        '100000098687': map { 'label': 'International Nonproprietary Names For Pharmaceutical Substances' },
        '100000107033': map { 'label': 'Maximum Residue Limit Species' },
        '100000108873': map { 'label': 'Target Species' },
        '100000111573': map { 'label': '4-letter ISO 3166-3 Codes for the representation of names of countries and their subdivisions' },
        '100000115254': map { 'label': 'Drug Information System of the National Competent Authorities in Germany' },
        '100000115255': map { 'label': 'VIKING PICA-UPD2' },
        '100000115256': map { 'label': 'THE MERCK INDEX 11TH EDITION' },
        '100000115257': map { 'label': 'MARTINDALE 35THEDITION' },
        '100000115258': map { 'label': 'MARTINDALE 35TH EDITION' },
        '100000115259': map { 'label': 'INVESTIGATOR BROCHURE GLPG0492' },
        '100000115260': map { 'label': 'OESTERREICHISCHES ARZNEIBUCH' },
        '100000115261': map { 'label': 'US PATENT 6818619 B2 BURNETT ET AL.' },
        '100000115262': map { 'label': 'MARKETING AUTHORISATION' },
        '100000115263': map { 'label': 'REG VERIFICATION' },
        '100000115264': map { 'label': 'BRITISH HERBAL PHARMACOPOEIA' },
        '100000115265': map { 'label': 'INVESTIGATOR''S BROCHURE GLPG0634' },
        '100000115266': map { 'label': 'PHEUR' },
        '100000115267': map { 'label': 'IMP DOSSIER FEXINIDAZOLE' },
        '100000115268': map { 'label': 'SRS - SUBSTANCE REGISTRATION SYSTEM' },
        '100000115269': map { 'label': 'HUMAN' },
        '100000115270': map { 'label': 'PANGENETICS UK LTD' },
        '100000115271': map { 'label': 'EU.PHARM.' },
        '100000115272': map { 'label': 'MARTINALE 36TH EDITION' },
        '100000116697': map { 'label': 'Anatomical Therapeutic Chemical classification system - Veterinary' },
        '100000124117': map { 'label': 'ITIS (INTEGRATED TAXONOMIC INFORMATION SYSTEM)' },
        '100000124118': map { 'label': 'SPONSOR' },
        '100000124119': map { 'label': 'MARTINDALE' },
        '100000124120': map { 'label': 'RO50453337' },
        '100000124121': map { 'label': 'INVESTIGATOR''S BROCHURE &amp; PROTOCOL' },
        '100000124122': map { 'label': 'CHEMICAL ORIGIN' },
        '100000124123': map { 'label': 'UNII CODE' },
        '100000124124': map { 'label': 'SRS SYNONYM' },
        '100000124125': map { 'label': 'CHEMIDPLUS' },
        '100000125793': map { 'label': 'CHEMICALBOOK' },
        '100000125794': map { 'label': 'CHEMDATAS' },
        '100000125795': map { 'label': 'BRAZILIAN PHARMOCHEMICALS PRODUCERS ASSOCIATION' },
        '100000125796': map { 'label': 'CHEMBLINK' },
        '100000125797': map { 'label': 'CHINESE PHARMACOPOEIA' },
        '100000125798': map { 'label': 'COMMUNITY HERBAL MONOGRAPHS' },
        '100000125799': map { 'label': 'COMMUNITY REGISTER OF THE EUROPEAN COMMISSION' },
        '100000125800': map { 'label': 'DAILYMED' },
        '100000125801': map { 'label': 'AMIS' },
        '100000125802': map { 'label': 'EUROPEAN DIRECTORATE FOR THE QUALITY OF MEDICINES &amp; HEALTHCARE (EDQM)' },
        '100000125803': map { 'label': 'HAGERS HANDBUCH DER DROGEN UND ARZNEISTOFFE' },
        '100000125804': map { 'label': 'INDEX NOMINUM' },
        '100000125805': map { 'label': 'INDEX PLANTARUM MEDICINALIUM TOTIUS MUNDI EORUMQUE SYNONYMORUM' },
        '100000125806': map { 'label': 'JAPANESE PHARMACEUTICAL MANUFACTURERS ASSOCIATION' },
        '100000125807': map { 'label': 'KEW GARDEN- IPLANTS DATABASE' },
        '100000125808': map { 'label': 'KEW GARDEN - WORLD CHECKLIST OF SELECTED PLANT NAMES' },
        '100000125809': map { 'label': 'PHARMACOPOEIA BOHEMOSLOVACA' },
        '100000125810': map { 'label': 'THE PLANT LIST' },
        '100000125811': map { 'label': 'VIRTUELLE FACHBIBLIOTHEK PHARMAZIE' },
        '100000125812': map { 'label': 'WHO RECOMMENDATION' },
        '100000125813': map { 'label': 'INTERNATIONAL CODE OF ZOOLOGICAL NOMENCLATURE (ICZN)' },
        '100000125814': map { 'label': 'INTERNATIONAL CODE OF BOTANICAL NOMENCLATURE (ICBN)' },
        '100000125815': map { 'label': 'INTERNATIONAL CODE OF NOMENCLATURE OF BACTERIA (ICNB)' },
        '100000125816': map { 'label': 'INTERNATIONAL CODE OF VIRUS CLASSIFICATION AND NOMENCLATURE' },
        '100000125817': map { 'label': 'GUIDELINE FOR HUMAN VACCINES (EMEA, CPMP)' },
        '100000125818': map { 'label': 'EV Single Substance' },
        '100000125819': map { 'label': 'EV Specified Substance Group 1' },
        '100000125820': map { 'label': 'EV Specified Substance Group 2' },
        '100000126407': map { 'label': 'EMA MONOGRAPH' },
        '100000126408': map { 'label': 'USP 25' },
        '100000126409': map { 'label': 'US PRESCRIBING INFORMATION' },
        '100000126410': map { 'label': 'DRUGS.COM' },
        '100000126411': map { 'label': 'ALNYLAM' },
        '100000126412': map { 'label': 'HERBAPOL LUBLIN SPECIFICATIONS' },
        '100000126413': map { 'label': 'CHUNANCY' },
        '100000126414': map { 'label': 'INVESTIGATOR''S BROCHURE GLPG0778' },
        '100000126415': map { 'label': 'HALOZYME INC.' },
        '100000126416': map { 'label': 'RELES.RU' },
        '100000126417': map { 'label': 'TRADITIONAL RECORD' },
        '100000126418': map { 'label': 'CORTHERA INC.' },
        '100000126419': map { 'label': 'CHEMICAL BOOK' },
        '100000126420': map { 'label': 'MARTINDALE 30TH' },
        '100000126427': map { 'label': 'AXELAR AB' },
        '100000126428': map { 'label': 'QLT INC.' },
        '100000126429': map { 'label': 'SUMMARY PRODUCT OF CHARACTERISTICS' },
        '100000127886': map { 'label': 'INTERNAL MONOGRAPHY' },
        '100000127906': map { 'label': 'HOMEOPATHIC PHARMACOPOEIA OF THE UNITED STATES' },
        '100000127907': map { 'label': 'DIRECTIVE 95/45/EC COLOURANTS' },
        '100000127908': map { 'label': 'BELGIAN PHARMACOPOEIA' },
        '100000127909': map { 'label': 'INTERNAL MONOGRAPH' },
        '100000127910': map { 'label': 'PH.EUR' },
        '100000127911': map { 'label': 'HSE' },
        '100000127912': map { 'label': 'GERMAN HOMOEOPATHIC PHARMACOPOEIA' },
        '100000127913': map { 'label': 'GERMAN HOMOEOPATHIC PHARMACOPOEIA 1934' },
        '100000129450': map { 'label': 'PH.HELV.' },
        '100000129451': map { 'label': 'PH. EU.' },
        '100000129452': map { 'label': 'FARMACOPEA EUROPEA' },
        '100000129453': map { 'label': 'HOMOOPATHISCHE ARZNEIBUCH' },
        '100000129454': map { 'label': 'BP' },
        '100000129467': map { 'label': 'COMMISSION DIRECTIVE 2008/128/EC COLOURANTS' },
        '100000129468': map { 'label': 'NORMA INTERNA' },
        '100000129530': map { 'label': 'USP/NF' },
        '100000129648': map { 'label': 'USF/NF' },
        '100000129742': map { 'label': 'SPECIFICATION NO S/018/HL' },
        '100000129743': map { 'label': 'CLASS NAMES AND THE INTERNATIONAL NUMBERING SYSTEM FOR FOOD ADDITIVES' },
        '100000129744': map { 'label': 'BRITISH PHARMACEUTICAL CODEX' },
        '100000129747': map { 'label': 'NF/EP' },
        '100000129748': map { 'label': 'DAC' },
        '100000129749': map { 'label': 'DAB / MADAUS GMBH' },
        '100000129750': map { 'label': 'EP / MADAUS GMBH' },
        '100000129751': map { 'label': 'MADAUS GMBH' },
        '100000129752': map { 'label': 'DAB' },
        '100000130087': map { 'label': 'F.U.' },
        '100000130088': map { 'label': 'INTERNAL REFERENCE' },
        '100000130089': map { 'label': 'US CODE OF FEDERAL REGULATIONS TITLE 21' },
        '100000130227': map { 'label': 'SPECIFICATION S/133/HL' },
        '100000130228': map { 'label': 'AUDEN MCKENZIE (PHARMA DIVISION) LTD' },
        '100000130229': map { 'label': 'SPECIFICATION NR P/002/HL' },
        '100000130589': map { 'label': 'FISHER, DEVLIN, GNABASIK CLASSIFICATION' },
        '100000130590': map { 'label': 'CLOVE LEAF OIL' },
        '100000130591': map { 'label': 'CAPSICIN' },
        '100000130592': map { 'label': 'SPECIFICATION NO S/109/HL' },
        '100000130593': map { 'label': 'SPECIFICATION NO S/110/HL' },
        '100000130594': map { 'label': 'SPECIFICATION NO S/172/HL' },
        '100000130595': map { 'label': 'SPECIFICATION NO S/205/HL' },
        '100000130596': map { 'label': 'SPECIFICATION NO S/214/HL' },
        '100000130607': map { 'label': 'COMMISSION DIRECTIVE 2008/84/EC' },
        '100000130608': map { 'label': 'PORTUGUESE PHARMACOPOEIA' },
        '100000130609': map { 'label': 'MANUFACTURER' },
        '100000130610': map { 'label': 'OAB' },
        '100000130611': map { 'label': 'REGULATION (EC) NO 1334/2008' },
        '100000130727': map { 'label': 'INHOUSE MONOGRAPH' },
        '100000130728': map { 'label': 'FOOD CHEMICAL CODEX' },
        '100000130729': map { 'label': 'HOMÖOPATHISCHES ARZNEIBUCH' },
        '100000130820': map { 'label': 'COMMISSIONS DIRECTIVE 2008/128/EC' },
        '100000130821': map { 'label': 'MONOGRAFIA INTERNA' },
        '100000131709': map { 'label': 'SMW 28640' },
        '100000131727': map { 'label': 'MARTINDLE' },
        '100000131728': map { 'label': 'IN-HOUSE SPECIFICATION' },
        '100000131729': map { 'label': 'SPECIFICATION NO S/052/HL' },
        '100000131730': map { 'label': 'SPECIFICATION NO S/081/HL' },
        '100000132503': map { 'label': 'SPECIFICATION NO S/032/HL' },
        '100000132504': map { 'label': 'PH.EUR. (MODIFIED)' },
        '100000132505': map { 'label': 'NOT SPECIFIED' },
        '100000132506': map { 'label': 'RECIPHARM' },
        '100000132507': map { 'label': 'FOOD GRADE' },
        '100000132508': map { 'label': 'LEO PHARMA' },
        '100000132509': map { 'label': 'WIKIPEDIA' },
        '100000132510': map { 'label': 'SPECIFICATION NO S/269/HL' },
        '100000132511': map { 'label': 'SPECIFICATION NO S/270/HL' },
        '100000132512': map { 'label': 'SPECIFICATION NO S/271/HL' },
        '100000132513': map { 'label': 'SPECIFICATION NO S/368/HL' },
        '100000132514': map { 'label': 'TURMERIC EXTRACT E100' },
        '100000132515': map { 'label': 'CORNISH CREAM FLAVOUR' },
        '100000132516': map { 'label': 'VANILLA MINT FLAVOUR' },
        '100000132517': map { 'label': 'SPECIFICATION NO S/487/HL' },
        '100000132518': map { 'label': 'SPECIFICATION NO S/486/HL' },
        '100000132519': map { 'label': 'SPECIFICATION NO S/491/HL' },
        '100000132520': map { 'label': 'ETOL' },
        '100000132521': map { 'label': 'SPECIFICATION NO P/017/HL' },
        '100000132522': map { 'label': 'SPECIFICATION NO S/066/HL' },
        '100000132523': map { 'label': 'SUBSTANCE MANUFACTURER SPECIFICATION' },
        '100000132881': map { 'label': 'ICH TOPIC Q6A:SPEC:TEST PROC &amp; ACCEPTANCE CRITERIA FOR NEW DS &amp; NEW DP' },
        '100000132902': map { 'label': 'SPECIFICATION NO S/152/HL' },
        '100000132903': map { 'label': 'SPECIFICATION NO S/153/HL' },
        '100000132904': map { 'label': 'SPECIFICATION NO S/082/HL' },
        '100000132905': map { 'label': 'SPECIFICATION NO S/572/HL' },
        '100000132977': map { 'label': 'IB' },
        '100000133377': map { 'label': 'INTERNATIONAL NUMBERING SYSTEM (INS) FOR FOOD ADDITIVES' },
        '100000133378': map { 'label': 'PHARMACOPOEIA HELVETICA' },
        '100000133379': map { 'label': 'SILESIA' },
        '100000133380': map { 'label': 'SPECIFICATION NO S/208/HL' },
        '100000133381': map { 'label': 'SPECIFICATION NO P/014/HL' },
        '100000133382': map { 'label': 'SPECIFICATION NO P/005/HL' },
        '100000133383': map { 'label': 'EUROPEAN PHARMACOPOEA' },
        '100000133384': map { 'label': 'SPECIFICATION NO S/145/HL' },
        '100000133385': map { 'label': 'FARMAKOPEA POLSKA' },
        '100000133386': map { 'label': 'SPECIFICATION NO S/199/HL' },
        '100000133530': map { 'label': 'COMPANY SPEC.' },
        '100000133531': map { 'label': 'MITSUBISHI-KAGAKU FOODS CORPORATION' },
        '100000133532': map { 'label': 'INTERNAL SPECIFICATIONS' },
        '100000133533': map { 'label': '5 - COMMISSION DIRECTIVE 2008/128/EC OF 22 DECEMBER 2008' },
        '100000133534': map { 'label': 'LABORATORI BALDACCI, SPA' },
        '100000133535': map { 'label': 'COLORCON ASIA PVT. LTD.' },
        '100000133536': map { 'label': 'QUEST INTERNATIONAL' },
        '100000133537': map { 'label': 'GIVANDAN SCHWEIZ AG LTD.' },
        '100000133538': map { 'label': 'FIRMENICH SA' },
        '100000133539': map { 'label': 'COMMISSION DIRECTIVE 2007/42/EC' },
        '100000133540': map { 'label': 'SPECIFICATION NO S/121/HL' },
        '100000133541': map { 'label': 'REGULATION ( EC ) NO 1334/ 2008' },
        '100000133542': map { 'label': 'COLORCON, LTD.' },
        '100000133543': map { 'label': 'IN HOUSE' },
        '100000133544': map { 'label': 'SPECIFICATION NO P/015/HL' },
        '100000133545': map { 'label': 'SPECIFICATION NO S/194/HL' },
        '100000133546': map { 'label': 'SPECIFICATION NO P/011/HL' },
        '100000133547': map { 'label': 'SPECIFICATION NO P/013/HL' },
        '100000133548': map { 'label': 'PH. EUR' },
        '100000133549': map { 'label': 'LANOLINES LA TOSSÉE' },
        '100000133550': map { 'label': 'COSTA E IRMÃOS, LDA.' },
        '100000133960': map { 'label': 'ASTA MEDICA' },
        '100000133961': map { 'label': 'SPEC. GEBRO' },
        '100000133962': map { 'label': 'SPEC. FINZELBERG' },
        '100000133963': map { 'label': 'SPEC. COLORCON' },
        '100000133964': map { 'label': 'SPEC. FIRMENICH' },
        '100000133967': map { 'label': 'SPECIFICATION NO S/216/HL' },
        '100000133968': map { 'label': 'HOMÖOPAHTISCHES ARZNEIBUCH (HAB)' },
        '100000133969': map { 'label': 'LUCTA S.A.' },
        '100000133970': map { 'label': 'JOH. VÖGELE KG' },
        '100000133971': map { 'label': 'COLORCON' },
        '100000133972': map { 'label': 'FRUTAROM' },
        '100000133973': map { 'label': 'GEBRO PHARMA GMBH' },
        '100000133974': map { 'label': 'DIRECTIVE 98/86/EC' },
        '100000133975': map { 'label': 'E 172' },
        '100000133976': map { 'label': 'CTD 3.2.S' },
        '100000133977': map { 'label': 'IN-HOUSE MONOGRAPH' },
        '100000134135': map { 'label': 'DIRECTIVE 95/45/EC' },
        '100000134407': map { 'label': 'GERMAN HOMEOPATHIC PHARMACOPOEIA' },
        '100000134408': map { 'label': 'GERMAN COMMISSION D MONOGRAPHS' },
        '100000134409': map { 'label': 'FDA INACTIVE INGREDIENTS DATABASE' },
        '100000134410': map { 'label': 'SRC574' },
        '100000134457': map { 'label': 'BRITISH PHARMACOPOEIAL CODEX (BPC)' },
        '100000134592': map { 'label': 'GREEK PHARMACOPOEIA' },
        '100000134822': map { 'label': 'NON-COMPENDIAL' },
        '100000134928': map { 'label': 'EVONIK' },
        '100000135368': map { 'label': 'CIRCULAR OF NATIONAL COMPETENT AUTHORITY' },
        '100000135612': map { 'label': 'SCHERING ITALY' },
        '100000136129': map { 'label': 'DEUTSCHER ARZNEIMITTEL CODEX' },
        '100000136249': map { 'label': 'MONOGRAPH TOPO PHARMA' },
        '100000136326': map { 'label': 'MONOGRAPH' },
        '100000136378': map { 'label': 'XEVMPD SUBSTANCES, VERSION 9.0' },
        '100000137075': map { 'label': 'HOMÖOPATHISCHES ARNEIBUCH' },
        '100000137076': map { 'label': 'HOMÖOPATHISCHES ARZNEIBUCH VON 1934' },
        '100000137077': map { 'label': 'PEKANA MONOGRAPHIE' },
        '100000137571': map { 'label': 'GILEAD-COLORCON-SPEC' },
        '100000137650': map { 'label': 'LEIBNIZ INST. DSMZ-GERMAN COLLECTION OF MICROORG. AND CELL CULTURES' },
        '100000138826': map { 'label': 'POLFA TARCHOMIN' },
        '100000138827': map { 'label': 'PHARMACOPOEIA HUNGARICA' },
        '100000138828': map { 'label': 'DIRECTIVE 76/768/EEC' },
        '100000138829': map { 'label': 'NOT RELEVANT' },
        '100000138830': map { 'label': 'SIFI MONOGRAPH' },
        '100000138831': map { 'label': 'CHATTEM GLOBAL CONSUMER PRODUCTS LTD.' },
        '100000138832': map { 'label': 'HERBAPOL WARSZAWA SP. Z O.O.' },
        '100000138833': map { 'label': 'PH.EUR.' },
        '100000139452': map { 'label': 'MAH''S SPECIFICATION' },
        '100000139643': map { 'label': 'DIRECTIVA 88/388/CE' },
        '100000140139': map { 'label': 'DUTCH PHARMACOPOEIA' },
        '100000140140': map { 'label': 'GERMAN PHARMACEUTICAL CODEX' },
        '100000140141': map { 'label': 'JAPANESE PHARMACEUTICAL EXCIPIENTS' },
        '100000140152': map { 'label': 'IN HOUSE TRADENAME' },
        '100000140478': map { 'label': 'GILEAD IN-HOUSE' },
        '100000140642': map { 'label': 'DAC DEUTSCHER ARZNEIMITTEL CODEX' },
        '100000141024': map { 'label': 'OTHER (NON PROPRIETARY NAME)' },
        '100000141208': map { 'label': 'BUNDESANZEIGER' },
        '100000141209': map { 'label': 'GERMAN COMISSION C MONOGRAPHS' },
        '100000141210': map { 'label': 'GERMAN COMMISSION C MONOGRAPHS' },
        '100000141224': map { 'label': 'GERMAN COMMISSION C MONOGRAPHS' },
        '100000141288': map { 'label': 'GERMAN DRUG LAW' },
        '100000141359': map { 'label': 'MONOGRAPHY BY PHARMA LIEBERMANN GMBH' },
        '100000141360': map { 'label': 'MONOGRAPHY OF PHARMA LIEBERMANN' },
        '100000141421': map { 'label': 'RIBBON S.R.L.' },
        '100000141869': map { 'label': 'AMOXICILOIL OCTA-L-LISINA' },
        '100000141870': map { 'label': 'EFEMA' },
        '100000142912': map { 'label': 'DEUTSCHER ARZNEIMITTEL-CODEX' },
        '100000143017': map { 'label': 'EURD LIST' },
        '100000143205': map { 'label': 'NO OFFICIAL MONOGRAPH' },
        '100000144535': map { 'label': 'SENSIENT FOOD COLORS GERMANY GMBH' },
        '100000144558': map { 'label': 'IFF-SPECIDICATION-10814421' },
        '100000144559': map { 'label': 'IFF-10814338' },
        '100000144560': map { 'label': 'FIRMENICH-501103AP0551' },
        '100000144849': map { 'label': 'HAB 1' },
        '100000144867': map { 'label': 'HAB' },
        '100000144881': map { 'label': 'HAB 2003' },
        '100000144882': map { 'label': 'HAB 2003 / HAB V. 2A' },
        '100000144950': map { 'label': 'HAB 34' },
        '100000144951': map { 'label': 'HAB 2003 / HAB V. 3A' },
        '100000144952': map { 'label': 'HAB 2003 / HAB V. 4A' },
        '100000145035': map { 'label': 'PRODUCT REGULATORY DATASHEET' },
        '100000145385': map { 'label': 'WHO PUBLICATION' },
        '100000146031': map { 'label': 'Annex I of the Commission Regulation (EC) No 1234/2008 of 24 November 2008 - Variations Regulation' },
        '100000146032': map { 'label': 'Annex of the Guideline on the details of the various categories of variation, as published on the 12th December 2008' },
        '100000146033': map { 'label': 'Annex of the Guideline on the details of the various categories of variation, as published on the 16th May 2013' },
        '100000146034': map { 'label': 'Application Form for a variation to a Marketing Authorisation' },
        '100000146035': map { 'value': 'SIAMED', 'label': 'SIAMED - EMA CP management system' },
        '100000151625': map { 'label': 'FEDERAL FOOD, DRUG AND COSMETIC ACT (FFDCA)' },
        '100000151708': map { 'label': 'IN HOUSE SPECIFICATIONS' },
        '100000151744': map { 'label': 'HOMOEOPATHISCHES ARZNEIBUCH (HAB)' },
        '100000151776': map { 'label': 'HOMOEOPATHIC PHARMACOPOEIA (HAB)' },
        '100000151869': map { 'label': 'FDA SUBSTANCE REGISTRATION SYSTEM' },
        '100000151893': map { 'label': 'INFAI' },
        '100000152989': map { 'label': 'CHEBI' },
        '100000153275': map { 'label': 'PUBCHEM' },
        '100000153730': map { 'label': 'electronic Common Technical Document' },
        '100000154910': map { 'label': 'DS-1040B IB' },
        '100000155628': map { 'label': 'TEST_SOURCE' },
        '100000155887': map { 'label': 'EC APPROVAL DECISION LETTER' },
        '100000155988': map { 'label': 'COMMITTEE OF ADVANCED THERAPY' },
        '100000156245': map { 'label': 'EEC FLAVOURING DIRECTIVE' },
        '100000156305': map { 'label': 'UNITED STATES PHARMACOPOEIA' },
        '100000156762': map { 'label': 'PHARMACOPOEIA' },
        '100000156899': map { 'label': 'USP/NATIONAL FORMULARY' },
        '100000156900': map { 'label': 'EUROPEAN PHARMACOPOEIA/NATIONAL FORMULARY' },
        '100000157360': map { 'label': 'USP-NF' },
        '100000157915': map { 'label': 'PACKAGE LEAFLET (PIL)' },
        '100000157979': map { 'label': 'EUROPEAN PHARMACO' },
        '100000158317': map { 'label': 'KARYOPHARM THERAPEUTICS INC.' },
        '100000158596': map { 'label': 'CHUCAEN' },
        '100000159416': map { 'label': 'EU PHARMACOPOEIA/US PHARMACOPOEIA/JP PHARMACOPOEIA' },
        '100000159417': map { 'label': 'DRUG MASTER FILE' },
        '100000159418': map { 'label': 'EU PHARMACOPOEIA/US PHARMACOPOEIA/NATIONAL FORMULARY/JP PHARMACOPOEIA' },
        '100000159429': map { 'label': 'ENCYCLOPEDIA OF LIFE (EOL)' },
        '100000159430': map { 'label': '1' },
        '100000159437': map { 'label': 'EU PHARMACOPOEIA' },
        '100000159480': map { 'label': 'EU PHARMACOPOEIA/COMPANY SPECIFICATION' },
        '100000159716': map { 'label': 'EUROPEAN PHARMACOPOEIA/COMPANY SPECIFICATION' },
        '100000159759': map { 'label': 'DMF' },
        '100000159798': map { 'label': 'INDIAN PHARMACOPOEIA' },
        '100000159951': map { 'label': 'NATIONAL FORMULARY/COMPANY SPECIFICATION' },
        '100000159988': map { 'label': 'EUROPEAN PHARMACOPOEIA/DMF' },
        '100000159989': map { 'label': 'USP/COMPANY SPECIFICATION' },
        '100000160900': map { 'label': 'HAB 2007 (HAB, V 4A)' },
        '100000164861': map { 'label': 'EV-CODE FOR TEV-50939' },
        '100000167113': map { 'label': 'CZECH PHARMACOPOEIA' },
        '100000167409': map { 'label': 'EudraVigilance Veterinary Medicinal Product Dictionary' },
        '100000167410': map { 'label': 'EudraVigilance Human – Active Directory' },
        '100000167412': map { 'label': 'EudraVigilance Vet – Active Directory' },
        '100000167429': map { 'label': 'European Inspections Database' },
        '100000167430': map { 'label': 'EMA Small and Medium-sized enterprise database' },
        '100000167431': map { 'label': 'EMA SAP Finance' },
        '100000167435': map { 'label': 'GS1 Global Location Number' },
        '100000167446': map { 'label': 'OMS Organisation Identifier' },
        '100000167449': map { 'label': 'Dun &amp; Bradstreet’s Data Universal Numbering System' },
        '100000167473': map { 'label': 'OMS Location Identifier' },
        '100000167474': map { 'label': 'OMS Request Identifier' },
        '100000167496': map { 'label': 'PHARMACOPÉE FRANÇAISE' },
        '100000167575': map { 'label': 'GS1 Global Trade Item Number' },
        '100000168506': map { 'label': 'EMA Eudra Common Directory' },
        '100000168928': map { 'label': 'TESTSOURCE3' },
        '100000169017': map { 'label': 'TESTSOURCE5' },
        '100000169811': map { 'label': 'EMA Small and Medium-sized enterprise number' },
        '100000169812': map { 'label': 'UCUM - Unified Code for Units of Measurement' },
        '100000169813': map { 'label': 'BfArM - Federal Institute for Drugs and Medical Devices' },
        '100000170407': map { 'label': 'Extended EudraVigilance Medicinal Product Dictionary organisation system key' },
        '100000170742': map { 'label': 'OMS Individual Identifier' },
        '100000170925': map { 'value': 'ECHA (EC/EINECS)', 'label': 'EUROPEAN CHEMICALS AGENCY (ECHA)' },
        '100000170926': map { 'label': 'COSING' },
        '100000170930': map { 'label': 'EXPANDED BONE MARROW-DERIVED HUMAN MESENCHYMAL STROMAL CELLS' },
        '100000170968': map { 'label': 'OMT-28_IB_20161104' },
        '100000172923': map { 'label': 'European Inspections Database organisation system key' },
        '200000003444': map { 'label': 'EudraVigilance Human Dynamics' },
        '200000005019': map { 'label': 'European Union Telematics Controlled Terms' },
        '200000005700': map { 'label': 'EudraVigilance Veterinary Dynamics' },
        '200000005825': map { 'label': 'HANDBOOK OF PHARMACEUTICAL MANUFACTURING FORMULATIONS' },
        '200000005826': map { 'label': 'PROTEIN DATA BANK (PDB)' },
        '200000005827': map { 'label': 'CHINA FOOD AND DRUG ADMINISTRATION' },
        '200000005828': map { 'label': 'INDEX FUNGORUM' },
        '200000005829': map { 'label': 'EUROPEAN FOOD SAFETY AUTHORITY (EFSA)' },
        '200000010646': map { 'label': 'Veterinary International Conference on Harmonization' },
        '200000010685': map { 'label': 'FOOD AND DRUG ADMINISTRATION' },
        '200000018817': map { 'value': 'FDA UNII', 'label': 'Unique Ingredient Identifier' },
        '200000022798': map { 'label': 'European Databank on Medical Devices' },
        '200000023337': map { 'label': 'EU Interinstitutional Style Guide' },
        '200000024893': map { 'label': 'Product Management Service' },
        '200000025171': map { 'label': 'Applicant’s document' },
        '200000025184': map { 'label': 'ADISINSIGHT' },
        '200000025197': map { 'value': 'SVG', 'label': 'Substance Validation Group' },
        '200000025232': map { 'label': '3-letter ISO 639-3 Codes for the representation of names of languages' },
        '200000026000': map { 'label': 'EMA SAP Vendor Identifier' },
        '200000026002': map { 'label': 'National Business Registry number' },
        '200000027029': map { 'label': 'Pharmacy Product Number' },
        '200000027030': map { 'label': 'National Trade Item Number' },
        '200000032418': map { 'value': 'ECHA (EC/EINECS)' }
    }"/>

    <!-- The term to write when more than one term maps to the same GSRS value.
         ``ema:index`` merges this last, so it wins over the derived choice.
         ECHA is the only GSRS name with two terms behind it, and 200000032418
         is the one the EMA documents carry. It carries no ``label``: it is not
         in ``rms_lists.csv`` and exists only so the code export writes the id
         the EMA documents use, so a citation has no reason to resolve to it. -->
    <xsl:variable name="ema-code-system-preferred" as="map(*)" select="map {
        'ECHA (EC/EINECS)': '200000032418'
    }"/>

    <!-- Turn a term map into ``value -> term id``, keyed by one of the fields.

         Used by the export side, which starts from a GSRS value and needs the
         term id to write. A term that carries no such field is left out: only
         some lists have a GSRS side at all, and a term that does not is
         reachable by its label alone.

         Entries are generated in ascending term id order and ``map:merge``
         keeps the last one, so the highest id wins where a field is not
         unique. The three argument form merges ``$preferred`` over the
         result, which lets a caller pin the id to write. -->
    <xsl:function name="ema:index" as="map(*)">
        <xsl:param name="terms" as="map(*)"/>
        <xsl:param name="key" as="xs:string"/>
        <xsl:sequence select="ema:index($terms, $key, map {})"/>
    </xsl:function>

    <xsl:function name="ema:index" as="map(*)">
        <xsl:param name="terms" as="map(*)"/>
        <xsl:param name="key" as="xs:string"/>
        <xsl:param name="preferred" as="map(*)"/>
        <xsl:sequence select="map:merge((
            map:merge(
                for-each(
                    sort(map:keys($terms)),
                    function($term) {
                        let $value := map:get($terms($term), $key)
                        return if (exists($value))
                            then map:entry($value, $term)
                            else ()
                    }),
                map { 'duplicates': 'use-last' }),
            $preferred
        ), map { 'duplicates': 'use-last' })"/>
    </xsl:function>

    <!-- The field a term id carries, e.g. the display name a term is written
         with. A term id the list does not carry has no field, which is how a
         caller tells the two apart and falls back. -->
    <xsl:function name="ema:term-field" as="xs:string?">
        <xsl:param name="terms" as="map(*)"/>
        <xsl:param name="term" as="xs:string"/>
        <xsl:param name="field" as="xs:string"/>
        <xsl:sequence select="map:get($terms($term), $field)"/>
    </xsl:function>

</xsl:stylesheet>
