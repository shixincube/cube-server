/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.util;

import java.util.HashMap;

/**
 * 文件类型。
 */
public enum FileType {

    FILE(new String[] { "" }, "application/octet-stream"),

    BMP(new String[]{ "bmp" }, "image/bmp", new byte[] { 0x42, 0x4d }),

    BZIP(new String[]{ "bz" }, "application/x-bzip", new byte[] { 0x42, 0x5a }),

    EXE(new String[]{ "exe" }, "application/x-msdownload", new byte[] { 0x4d, 0x5a }),

    FIG(new String[]{ "fig" }, "application/x-xfig", new byte[] { 0x23, 0x46, 0x49, 0x47 }),

    FITS(new String[]{ "fits" }, "application/fits", new byte[] { 0x53, 0x49, 0x4d, 0x50, 0x4c, 0x45 }),

    GIF(new String[]{ "gif" }, "image/gif", new byte[] { 0x47, 0x49, 0x46, 0x38 }),

    GZIP(new String[]{ "gz" }, "application/x-gzip", new byte[] { 0x1f, (byte)0x8b }),

    RGB(new String[]{ "rgb" }, "image/x-rgb", new byte[] { 0x01, (byte)0xda }),

    JPEG(new String[]{ "jpeg", "jpg", "jpe" }, "image/jpeg", new byte[] { (byte)0xff, (byte)0xd8, (byte)0xff, (byte)0xe0 }),

    NIFF(new String[]{ "nif" }, "application/vnd.music-niff", new byte[] { 0x49, 0x49, 0x4e, 0x31 }),

    PM(new String[]{ "pm" }, "application/x-pm", new byte[] { 0x56, 0x49, 0x45, 0x57 }),

    PNG(new String[]{ "png" }, "image/png", new byte[] { (byte)0x89, 0x50, 0x4e, 0x47 }),

    TAR(new String[]{ "tar" }, "application/x-tar", new byte[] { 0x75, 0x73, 0x74, 0x61, 0x72 }),

    TIFF_BE(new String[]{ "tiff", "tif" }, "image/tiff", new byte[] { 0x4d, 0x4d, 0x00, 0x2a }),

    TIFF_LE(new String[]{ "tiff", "tif" }, "image/tiff", new byte[] { 0x49, 0x49, 0x2a, 0x00 }),

    XCF(new String[]{ "xcf" }, "application/x-xcf", new byte[] { 0x67, 0x69, 0x6d, 0x70, 0x20, 0x78, 0x63, 0x66, 0x20, 0x76 }),

    XPM(new String[]{ "xpm" }, "image/x-xpixmap", new byte[] { 0x2f, 0x2a, 0x20, 0x58, 0x50, 0x4d, 0x20, 0x2a, 0x2f }),

    Z(new String[]{ "Z" }, "application/octet-stream", new byte[] { 0x1f, (byte)0x9d }),

    ZIP(new String[]{ "zip" }, "application/zip", new byte[] { 0x50, 0x4b, 0x03, 0x04 }),


    // 以下代码为机器自动生成

    EZ(new String[]{ "ez" }, "application/andrew-inset"),
    AW(new String[]{ "aw" }, "application/applixware"),
    ATOM(new String[]{ "atom" }, "application/atom+xml"),
    ATOMCAT(new String[]{ "atomcat" }, "application/atomcat+xml"),
    ATOMSVC(new String[]{ "atomsvc" }, "application/atomsvc+xml"),
    CCXML(new String[]{ "ccxml" }, "application/ccxml+xml"),
    CDMIA(new String[]{ "cdmia" }, "application/cdmi-capability"),
    CDMIC(new String[]{ "cdmic" }, "application/cdmi-container"),
    CDMID(new String[]{ "cdmid" }, "application/cdmi-domain"),
    CDMIO(new String[]{ "cdmio" }, "application/cdmi-object"),
    CDMIQ(new String[]{ "cdmiq" }, "application/cdmi-queue"),
    CU(new String[]{ "cu" }, "application/cu-seeme"),
    DAVMOUNT(new String[]{ "davmount" }, "application/davmount+xml"),
    DBK(new String[]{ "dbk" }, "application/docbook+xml"),
    DSSC(new String[]{ "dssc" }, "application/dssc+der"),
    XDSSC(new String[]{ "xdssc" }, "application/dssc+xml"),
    ECMA(new String[]{ "ecma" }, "application/ecmascript"),
    EMMA(new String[]{ "emma" }, "application/emma+xml"),
    EPUB(new String[]{ "epub" }, "application/epub+zip"),
    EXI(new String[]{ "exi" }, "application/exi"),
    PFR(new String[]{ "pfr" }, "application/font-tdpfr"),
    GML(new String[]{ "gml" }, "application/gml+xml"),
    GPX(new String[]{ "gpx" }, "application/gpx+xml"),
    GXF(new String[]{ "gxf" }, "application/gxf"),
    STK(new String[]{ "stk" }, "application/hyperstudio"),
    INK(new String[]{ "ink" }, "application/inkml+xml"),
    INKML(new String[]{ "inkml" }, "application/inkml+xml"),
    IPFIX(new String[]{ "ipfix" }, "application/ipfix"),
    JAR(new String[]{ "jar" }, "application/java-archive"),
    SER(new String[]{ "ser" }, "application/java-serialized-object"),
    CLASS(new String[]{ "class" }, "application/java-vm"),
    JS(new String[]{ "js" }, "application/javascript"),
    JSON(new String[]{ "json" }, "application/json"),
    JSONML(new String[]{ "jsonml" }, "application/jsonml+json"),
    LOSTXML(new String[]{ "lostxml" }, "application/lost+xml"),
    HQX(new String[]{ "hqx" }, "application/mac-binhex40"),
    CPT(new String[]{ "cpt" }, "application/mac-compactpro"),
    MADS(new String[]{ "mads" }, "application/mads+xml"),
    MRC(new String[]{ "mrc" }, "application/marc"),
    MRCX(new String[]{ "mrcx" }, "application/marcxml+xml"),
    MA(new String[]{ "ma" }, "application/mathematica"),
    NB(new String[]{ "nb" }, "application/mathematica"),
    MB(new String[]{ "mb" }, "application/mathematica"),
    MATHML(new String[]{ "mathml" }, "application/mathml+xml"),
    MBOX(new String[]{ "mbox" }, "application/mbox"),
    MSCML(new String[]{ "mscml" }, "application/mediaservercontrol+xml"),
    METALINK(new String[]{ "metalink" }, "application/metalink+xml"),
    META4(new String[]{ "meta4" }, "application/metalink4+xml"),
    METS(new String[]{ "mets" }, "application/mets+xml"),
    MODS(new String[]{ "mods" }, "application/mods+xml"),
    M21(new String[]{ "m21" }, "application/mp21"),
    MP21(new String[]{ "mp21" }, "application/mp21"),
    MP4S(new String[]{ "mp4s" }, "application/mp4"),
    DOC(new String[]{ "doc" }, "application/msword"),
    DOT(new String[]{ "dot" }, "application/msword"),
    MXF(new String[]{ "mxf" }, "application/mxf"),
    BIN(new String[]{ "bin" }, "application/octet-stream"),
    DMS(new String[]{ "dms" }, "application/octet-stream"),
    LRF(new String[]{ "lrf" }, "application/octet-stream"),
    MAR(new String[]{ "mar" }, "application/octet-stream"),
    SO(new String[]{ "so" }, "application/octet-stream"),
    DIST(new String[]{ "dist" }, "application/octet-stream"),
    DISTZ(new String[]{ "distz" }, "application/octet-stream"),
    PKG(new String[]{ "pkg" }, "application/octet-stream"),
    BPK(new String[]{ "bpk" }, "application/octet-stream"),
    DUMP(new String[]{ "dump" }, "application/octet-stream"),
    ELC(new String[]{ "elc" }, "application/octet-stream"),
    DEPLOY(new String[]{ "deploy" }, "application/octet-stream"),
    ODA(new String[]{ "oda" }, "application/oda"),
    OPF(new String[]{ "opf" }, "application/oebps-package+xml"),
    OGX(new String[]{ "ogx" }, "application/ogg"),
    OMDOC(new String[]{ "omdoc" }, "application/omdoc+xml"),
    ONETOC(new String[]{ "onetoc" }, "application/onenote"),
    ONETOC2(new String[]{ "onetoc2" }, "application/onenote"),
    ONETMP(new String[]{ "onetmp" }, "application/onenote"),
    ONEPKG(new String[]{ "onepkg" }, "application/onenote"),
    OXPS(new String[]{ "oxps" }, "application/oxps"),
    XER(new String[]{ "xer" }, "application/patch-ops-error+xml"),
    PDF(new String[]{ "pdf" }, "application/pdf"),
    PGP(new String[]{ "pgp" }, "application/pgp-encrypted"),
    ASC(new String[]{ "asc" }, "application/pgp-signature"),
    SIG(new String[]{ "sig" }, "application/pgp-signature"),
    PRF(new String[]{ "prf" }, "application/pics-rules"),
    P10(new String[]{ "p10" }, "application/pkcs10"),
    P7M(new String[]{ "p7m" }, "application/pkcs7-mime"),
    P7C(new String[]{ "p7c" }, "application/pkcs7-mime"),
    P7S(new String[]{ "p7s" }, "application/pkcs7-signature"),
    P8(new String[]{ "p8" }, "application/pkcs8"),
    AC(new String[]{ "ac" }, "application/pkix-attr-cert"),
    CER(new String[]{ "cer" }, "application/pkix-cert"),
    CRL(new String[]{ "crl" }, "application/pkix-crl"),
    PKIPATH(new String[]{ "pkipath" }, "application/pkix-pkipath"),
    PKI(new String[]{ "pki" }, "application/pkixcmp"),
    PLS(new String[]{ "pls" }, "application/pls+xml"),
    AI(new String[]{ "ai" }, "application/postscript"),
    EPS(new String[]{ "eps" }, "application/postscript"),
    PS(new String[]{ "ps" }, "application/postscript"),
    CWW(new String[]{ "cww" }, "application/prs.cww"),
    PSKCXML(new String[]{ "pskcxml" }, "application/pskc+xml"),
    RDF(new String[]{ "rdf" }, "application/rdf+xml"),
    RIF(new String[]{ "rif" }, "application/reginfo+xml"),
    RNC(new String[]{ "rnc" }, "application/relax-ng-compact-syntax"),
    RL(new String[]{ "rl" }, "application/resource-lists+xml"),
    RLD(new String[]{ "rld" }, "application/resource-lists-diff+xml"),
    RS(new String[]{ "rs" }, "application/rls-services+xml"),
    GBR(new String[]{ "gbr" }, "application/rpki-ghostbusters"),
    MFT(new String[]{ "mft" }, "application/rpki-manifest"),
    ROA(new String[]{ "roa" }, "application/rpki-roa"),
    RSD(new String[]{ "rsd" }, "application/rsd+xml"),
    RSS(new String[]{ "rss" }, "application/rss+xml"),
    RTF(new String[]{ "rtf" }, "application/rtf"),
    SBML(new String[]{ "sbml" }, "application/sbml+xml"),
    SCQ(new String[]{ "scq" }, "application/scvp-cv-request"),
    SCS(new String[]{ "scs" }, "application/scvp-cv-response"),
    SPQ(new String[]{ "spq" }, "application/scvp-vp-request"),
    SPP(new String[]{ "spp" }, "application/scvp-vp-response"),
    SDP(new String[]{ "sdp" }, "application/sdp"),
    SETPAY(new String[]{ "setpay" }, "application/set-payment-initiation"),
    SETREG(new String[]{ "setreg" }, "application/set-registration-initiation"),
    SHF(new String[]{ "shf" }, "application/shf+xml"),
    SMI(new String[]{ "smi" }, "application/smil+xml"),
    SMIL(new String[]{ "smil" }, "application/smil+xml"),
    RQ(new String[]{ "rq" }, "application/sparql-query"),
    SRX(new String[]{ "srx" }, "application/sparql-results+xml"),
    GRAM(new String[]{ "gram" }, "application/srgs"),
    GRXML(new String[]{ "grxml" }, "application/srgs+xml"),
    SRU(new String[]{ "sru" }, "application/sru+xml"),
    SSDL(new String[]{ "ssdl" }, "application/ssdl+xml"),
    SSML(new String[]{ "ssml" }, "application/ssml+xml"),
    TEI(new String[]{ "tei" }, "application/tei+xml"),
    TEICORPUS(new String[]{ "teicorpus" }, "application/tei+xml"),
    TFI(new String[]{ "tfi" }, "application/thraud+xml"),
    TSD(new String[]{ "tsd" }, "application/timestamped-data"),
    PLB(new String[]{ "plb" }, "application/vnd.3gpp.pic-bw-large"),
    PSB(new String[]{ "psb" }, "application/vnd.3gpp.pic-bw-small"),
    PVB(new String[]{ "pvb" }, "application/vnd.3gpp.pic-bw-var"),
    TCAP(new String[]{ "tcap" }, "application/vnd.3gpp2.tcap"),
    PWN(new String[]{ "pwn" }, "application/vnd.3m.post-it-notes"),
    ASO(new String[]{ "aso" }, "application/vnd.accpac.simply.aso"),
    IMP(new String[]{ "imp" }, "application/vnd.accpac.simply.imp"),
    ACU(new String[]{ "acu" }, "application/vnd.acucobol"),
    ATC(new String[]{ "atc" }, "application/vnd.acucorp"),
    ACUTC(new String[]{ "acutc" }, "application/vnd.acucorp"),
    AIR(new String[]{ "air" }, "application/vnd.adobe.air-application-installer-package+zip"),
    FCDT(new String[]{ "fcdt" }, "application/vnd.adobe.formscentral.fcdt"),
    FXP(new String[]{ "fxp" }, "application/vnd.adobe.fxp"),
    FXPL(new String[]{ "fxpl" }, "application/vnd.adobe.fxp"),
    XDP(new String[]{ "xdp" }, "application/vnd.adobe.xdp+xml"),
    XFDF(new String[]{ "xfdf" }, "application/vnd.adobe.xfdf"),
    AHEAD(new String[]{ "ahead" }, "application/vnd.ahead.space"),
    AZF(new String[]{ "azf" }, "application/vnd.airzip.filesecure.azf"),
    AZS(new String[]{ "azs" }, "application/vnd.airzip.filesecure.azs"),
    AZW(new String[]{ "azw" }, "application/vnd.amazon.ebook"),
    ACC(new String[]{ "acc" }, "application/vnd.americandynamics.acc"),
    AMI(new String[]{ "ami" }, "application/vnd.amiga.ami"),
    APK(new String[]{ "apk" }, "application/vnd.android.package-archive"),
    CII(new String[]{ "cii" }, "application/vnd.anser-web-certificate-issue-initiation"),
    FTI(new String[]{ "fti" }, "application/vnd.anser-web-funds-transfer-initiation"),
    ATX(new String[]{ "atx" }, "application/vnd.antix.game-component"),
    MPKG(new String[]{ "mpkg" }, "application/vnd.apple.installer+xml"),
    M3U8(new String[]{ "m3u8" }, "application/vnd.apple.mpegurl"),
    SWI(new String[]{ "swi" }, "application/vnd.aristanetworks.swi"),
    IOTA(new String[]{ "iota" }, "application/vnd.astraea-software.iota"),
    AEP(new String[]{ "aep" }, "application/vnd.audiograph"),
    MPM(new String[]{ "mpm" }, "application/vnd.blueice.multipass"),
    BMI(new String[]{ "bmi" }, "application/vnd.bmi"),
    REP(new String[]{ "rep" }, "application/vnd.businessobjects"),
    CDXML(new String[]{ "cdxml" }, "application/vnd.chemdraw+xml"),
    MMD(new String[]{ "mmd" }, "application/vnd.chipnuts.karaoke-mmd"),
    CDY(new String[]{ "cdy" }, "application/vnd.cinderella"),
    CLA(new String[]{ "cla" }, "application/vnd.claymore"),
    RP9(new String[]{ "rp9" }, "application/vnd.cloanto.rp9"),
    C4G(new String[]{ "c4g" }, "application/vnd.clonk.c4group"),
    C4D(new String[]{ "c4d" }, "application/vnd.clonk.c4group"),
    C4F(new String[]{ "c4f" }, "application/vnd.clonk.c4group"),
    C4P(new String[]{ "c4p" }, "application/vnd.clonk.c4group"),
    C4U(new String[]{ "c4u" }, "application/vnd.clonk.c4group"),
    C11AMC(new String[]{ "c11amc" }, "application/vnd.cluetrust.cartomobile-config"),
    C11AMZ(new String[]{ "c11amz" }, "application/vnd.cluetrust.cartomobile-config-pkg"),
    CSP(new String[]{ "csp" }, "application/vnd.commonspace"),
    CDBCMSG(new String[]{ "cdbcmsg" }, "application/vnd.contact.cmsg"),
    CMC(new String[]{ "cmc" }, "application/vnd.cosmocaller"),
    CLKX(new String[]{ "clkx" }, "application/vnd.crick.clicker"),
    CLKK(new String[]{ "clkk" }, "application/vnd.crick.clicker.keyboard"),
    CLKP(new String[]{ "clkp" }, "application/vnd.crick.clicker.palette"),
    CLKT(new String[]{ "clkt" }, "application/vnd.crick.clicker.template"),
    CLKW(new String[]{ "clkw" }, "application/vnd.crick.clicker.wordbank"),
    WBS(new String[]{ "wbs" }, "application/vnd.criticaltools.wbs+xml"),
    PML(new String[]{ "pml" }, "application/vnd.ctc-posml"),
    PPD(new String[]{ "ppd" }, "application/vnd.cups-ppd"),
    CAR(new String[]{ "car" }, "application/vnd.curl.car"),
    PCURL(new String[]{ "pcurl" }, "application/vnd.curl.pcurl"),
    DART(new String[]{ "dart" }, "application/vnd.dart"),
    RDZ(new String[]{ "rdz" }, "application/vnd.data-vision.rdz"),
    UVF(new String[]{ "uvf" }, "application/vnd.dece.data"),
    UVVF(new String[]{ "uvvf" }, "application/vnd.dece.data"),
    UVD(new String[]{ "uvd" }, "application/vnd.dece.data"),
    UVVD(new String[]{ "uvvd" }, "application/vnd.dece.data"),
    UVT(new String[]{ "uvt" }, "application/vnd.dece.ttml+xml"),
    UVVT(new String[]{ "uvvt" }, "application/vnd.dece.ttml+xml"),
    UVX(new String[]{ "uvx" }, "application/vnd.dece.unspecified"),
    UVVX(new String[]{ "uvvx" }, "application/vnd.dece.unspecified"),
    UVZ(new String[]{ "uvz" }, "application/vnd.dece.zip"),
    UVVZ(new String[]{ "uvvz" }, "application/vnd.dece.zip"),
    FE_LAUNCH(new String[]{ "fe_launch" }, "application/vnd.denovo.fcselayout-link"),
    DNA(new String[]{ "dna" }, "application/vnd.dna"),
    MLP(new String[]{ "mlp" }, "application/vnd.dolby.mlp"),
    DPG(new String[]{ "dpg" }, "application/vnd.dpgraph"),
    DFAC(new String[]{ "dfac" }, "application/vnd.dreamfactory"),
    KPXX(new String[]{ "kpxx" }, "application/vnd.ds-keypoint"),
    AIT(new String[]{ "ait" }, "application/vnd.dvb.ait"),
    SVC(new String[]{ "svc" }, "application/vnd.dvb.service"),
    GEO(new String[]{ "geo" }, "application/vnd.dynageo"),
    MAG(new String[]{ "mag" }, "application/vnd.ecowin.chart"),
    NML(new String[]{ "nml" }, "application/vnd.enliven"),
    ESF(new String[]{ "esf" }, "application/vnd.epson.esf"),
    MSF(new String[]{ "msf" }, "application/vnd.epson.msf"),
    QAM(new String[]{ "qam" }, "application/vnd.epson.quickanime"),
    SLT(new String[]{ "slt" }, "application/vnd.epson.salt"),
    SSF(new String[]{ "ssf" }, "application/vnd.epson.ssf"),
    ES3(new String[]{ "es3" }, "application/vnd.eszigno3+xml"),
    ET3(new String[]{ "et3" }, "application/vnd.eszigno3+xml"),
    EZ2(new String[]{ "ez2" }, "application/vnd.ezpix-album"),
    EZ3(new String[]{ "ez3" }, "application/vnd.ezpix-package"),
    FDF(new String[]{ "fdf" }, "application/vnd.fdf"),
    MSEED(new String[]{ "mseed" }, "application/vnd.fdsn.mseed"),
    SEED(new String[]{ "seed" }, "application/vnd.fdsn.seed"),
    DATALESS(new String[]{ "dataless" }, "application/vnd.fdsn.seed"),
    GPH(new String[]{ "gph" }, "application/vnd.flographit"),
    FTC(new String[]{ "ftc" }, "application/vnd.fluxtime.clip"),
    FM(new String[]{ "fm" }, "application/vnd.framemaker"),
    FRAME(new String[]{ "frame" }, "application/vnd.framemaker"),
    MAKER(new String[]{ "maker" }, "application/vnd.framemaker"),
    BOOK(new String[]{ "book" }, "application/vnd.framemaker"),
    FNC(new String[]{ "fnc" }, "application/vnd.frogans.fnc"),
    LTF(new String[]{ "ltf" }, "application/vnd.frogans.ltf"),
    FSC(new String[]{ "fsc" }, "application/vnd.fsc.weblaunch"),
    OAS(new String[]{ "oas" }, "application/vnd.fujitsu.oasys"),
    OA2(new String[]{ "oa2" }, "application/vnd.fujitsu.oasys2"),
    OA3(new String[]{ "oa3" }, "application/vnd.fujitsu.oasys3"),
    FG5(new String[]{ "fg5" }, "application/vnd.fujitsu.oasysgp"),
    BH2(new String[]{ "bh2" }, "application/vnd.fujitsu.oasysprs"),
    DDD(new String[]{ "ddd" }, "application/vnd.fujixerox.ddd"),
    XDW(new String[]{ "xdw" }, "application/vnd.fujixerox.docuworks"),
    XBD(new String[]{ "xbd" }, "application/vnd.fujixerox.docuworks.binder"),
    FZS(new String[]{ "fzs" }, "application/vnd.fuzzysheet"),
    TXD(new String[]{ "txd" }, "application/vnd.genomatix.tuxedo"),
    GGB(new String[]{ "ggb" }, "application/vnd.geogebra.file"),
    GGT(new String[]{ "ggt" }, "application/vnd.geogebra.tool"),
    GEX(new String[]{ "gex" }, "application/vnd.geometry-explorer"),
    GRE(new String[]{ "gre" }, "application/vnd.geometry-explorer"),
    GXT(new String[]{ "gxt" }, "application/vnd.geonext"),
    G2W(new String[]{ "g2w" }, "application/vnd.geoplan"),
    G3W(new String[]{ "g3w" }, "application/vnd.geospace"),
    GMX(new String[]{ "gmx" }, "application/vnd.gmx"),
    KML(new String[]{ "kml" }, "application/vnd.google-earth.kml+xml"),
    KMZ(new String[]{ "kmz" }, "application/vnd.google-earth.kmz"),
    GQF(new String[]{ "gqf" }, "application/vnd.grafeq"),
    GQS(new String[]{ "gqs" }, "application/vnd.grafeq"),
    GAC(new String[]{ "gac" }, "application/vnd.groove-account"),
    GHF(new String[]{ "ghf" }, "application/vnd.groove-help"),
    GIM(new String[]{ "gim" }, "application/vnd.groove-identity-message"),
    GRV(new String[]{ "grv" }, "application/vnd.groove-injector"),
    GTM(new String[]{ "gtm" }, "application/vnd.groove-tool-message"),
    TPL(new String[]{ "tpl" }, "application/vnd.groove-tool-template"),
    VCG(new String[]{ "vcg" }, "application/vnd.groove-vcard"),
    HAL(new String[]{ "hal" }, "application/vnd.hal+xml"),
    ZMM(new String[]{ "zmm" }, "application/vnd.handheld-entertainment+xml"),
    HBCI(new String[]{ "hbci" }, "application/vnd.hbci"),
    LES(new String[]{ "les" }, "application/vnd.hhe.lesson-player"),
    HPGL(new String[]{ "hpgl" }, "application/vnd.hp-hpgl"),
    HPID(new String[]{ "hpid" }, "application/vnd.hp-hpid"),
    HPS(new String[]{ "hps" }, "application/vnd.hp-hps"),
    JLT(new String[]{ "jlt" }, "application/vnd.hp-jlyt"),
    PCL(new String[]{ "pcl" }, "application/vnd.hp-pcl"),
    PCLXL(new String[]{ "pclxl" }, "application/vnd.hp-pclxl"),
    SFD_HDSTX(new String[]{ "sfd-hdstx" }, "application/vnd.hydrostatix.sof-data"),
    MPY(new String[]{ "mpy" }, "application/vnd.ibm.minipay"),
    AFP(new String[]{ "afp" }, "application/vnd.ibm.modcap"),
    LISTAFP(new String[]{ "listafp" }, "application/vnd.ibm.modcap"),
    LIST3820(new String[]{ "list3820" }, "application/vnd.ibm.modcap"),
    IRM(new String[]{ "irm" }, "application/vnd.ibm.rights-management"),
    SC(new String[]{ "sc" }, "application/vnd.ibm.secure-container"),
    ICC(new String[]{ "icc" }, "application/vnd.iccprofile"),
    ICM(new String[]{ "icm" }, "application/vnd.iccprofile"),
    IGL(new String[]{ "igl" }, "application/vnd.igloader"),
    IVP(new String[]{ "ivp" }, "application/vnd.immervision-ivp"),
    IVU(new String[]{ "ivu" }, "application/vnd.immervision-ivu"),
    IGM(new String[]{ "igm" }, "application/vnd.insors.igm"),
    XPW(new String[]{ "xpw" }, "application/vnd.intercon.formnet"),
    XPX(new String[]{ "xpx" }, "application/vnd.intercon.formnet"),
    I2G(new String[]{ "i2g" }, "application/vnd.intergeo"),
    QBO(new String[]{ "qbo" }, "application/vnd.intu.qbo"),
    QFX(new String[]{ "qfx" }, "application/vnd.intu.qfx"),
    RCPROFILE(new String[]{ "rcprofile" }, "application/vnd.ipunplugged.rcprofile"),
    IRP(new String[]{ "irp" }, "application/vnd.irepository.package+xml"),
    XPR(new String[]{ "xpr" }, "application/vnd.is-xpr"),
    FCS(new String[]{ "fcs" }, "application/vnd.isac.fcs"),
    JAM(new String[]{ "jam" }, "application/vnd.jam"),
    RMS(new String[]{ "rms" }, "application/vnd.jcp.javame.midlet-rms"),
    JISP(new String[]{ "jisp" }, "application/vnd.jisp"),
    JODA(new String[]{ "joda" }, "application/vnd.joost.joda-archive"),
    KTZ(new String[]{ "ktz" }, "application/vnd.kahootz"),
    KTR(new String[]{ "ktr" }, "application/vnd.kahootz"),
    KARBON(new String[]{ "karbon" }, "application/vnd.kde.karbon"),
    CHRT(new String[]{ "chrt" }, "application/vnd.kde.kchart"),
    KFO(new String[]{ "kfo" }, "application/vnd.kde.kformula"),
    FLW(new String[]{ "flw" }, "application/vnd.kde.kivio"),
    KON(new String[]{ "kon" }, "application/vnd.kde.kontour"),
    KPR(new String[]{ "kpr" }, "application/vnd.kde.kpresenter"),
    KPT(new String[]{ "kpt" }, "application/vnd.kde.kpresenter"),
    KSP(new String[]{ "ksp" }, "application/vnd.kde.kspread"),
    KWD(new String[]{ "kwd" }, "application/vnd.kde.kword"),
    KWT(new String[]{ "kwt" }, "application/vnd.kde.kword"),
    HTKE(new String[]{ "htke" }, "application/vnd.kenameaapp"),
    KIA(new String[]{ "kia" }, "application/vnd.kidspiration"),
    KNE(new String[]{ "kne" }, "application/vnd.kinar"),
    KNP(new String[]{ "knp" }, "application/vnd.kinar"),
    SKP(new String[]{ "skp" }, "application/vnd.koan"),
    SKD(new String[]{ "skd" }, "application/vnd.koan"),
    SKT(new String[]{ "skt" }, "application/vnd.koan"),
    SKM(new String[]{ "skm" }, "application/vnd.koan"),
    SSE(new String[]{ "sse" }, "application/vnd.kodak-descriptor"),
    LASXML(new String[]{ "lasxml" }, "application/vnd.las.las+xml"),
    LBD(new String[]{ "lbd" }, "application/vnd.llamagraphics.life-balance.desktop"),
    LBE(new String[]{ "lbe" }, "application/vnd.llamagraphics.life-balance.exchange+xml"),
    _123(new String[]{ "123" }, "application/vnd.lotus-1-2-3"),
    APR(new String[]{ "apr" }, "application/vnd.lotus-approach"),
    PRE(new String[]{ "pre" }, "application/vnd.lotus-freelance"),
    NSF(new String[]{ "nsf" }, "application/vnd.lotus-notes"),
    ORG(new String[]{ "org" }, "application/vnd.lotus-organizer"),
    SCM(new String[]{ "scm" }, "application/vnd.lotus-screencam"),
    LWP(new String[]{ "lwp" }, "application/vnd.lotus-wordpro"),
    PORTPKG(new String[]{ "portpkg" }, "application/vnd.macports.portpkg"),
    MCD(new String[]{ "mcd" }, "application/vnd.mcd"),
    MC1(new String[]{ "mc1" }, "application/vnd.medcalcdata"),
    CDKEY(new String[]{ "cdkey" }, "application/vnd.mediastation.cdkey"),
    MWF(new String[]{ "mwf" }, "application/vnd.mfer"),
    MFM(new String[]{ "mfm" }, "application/vnd.mfmp"),
    FLO(new String[]{ "flo" }, "application/vnd.micrografx.flo"),
    IGX(new String[]{ "igx" }, "application/vnd.micrografx.igx"),
    MIF(new String[]{ "mif" }, "application/vnd.mif"),
    DAF(new String[]{ "daf" }, "application/vnd.mobius.daf"),
    DIS(new String[]{ "dis" }, "application/vnd.mobius.dis"),
    MBK(new String[]{ "mbk" }, "application/vnd.mobius.mbk"),
    MQY(new String[]{ "mqy" }, "application/vnd.mobius.mqy"),
    MSL(new String[]{ "msl" }, "application/vnd.mobius.msl"),
    PLC(new String[]{ "plc" }, "application/vnd.mobius.plc"),
    TXF(new String[]{ "txf" }, "application/vnd.mobius.txf"),
    MPN(new String[]{ "mpn" }, "application/vnd.mophun.application"),
    MPC(new String[]{ "mpc" }, "application/vnd.mophun.certificate"),
    XUL(new String[]{ "xul" }, "application/vnd.mozilla.xul+xml"),
    CIL(new String[]{ "cil" }, "application/vnd.ms-artgalry"),
    CAB(new String[]{ "cab" }, "application/vnd.ms-cab-compressed"),
    XLS(new String[]{ "xls" }, "application/vnd.ms-excel"),
    XLM(new String[]{ "xlm" }, "application/vnd.ms-excel"),
    XLA(new String[]{ "xla" }, "application/vnd.ms-excel"),
    XLC(new String[]{ "xlc" }, "application/vnd.ms-excel"),
    XLT(new String[]{ "xlt" }, "application/vnd.ms-excel"),
    XLW(new String[]{ "xlw" }, "application/vnd.ms-excel"),
    XLAM(new String[]{ "xlam" }, "application/vnd.ms-excel.addin.macroenabled.12"),
    XLSB(new String[]{ "xlsb" }, "application/vnd.ms-excel.sheet.binary.macroenabled.12"),
    XLSM(new String[]{ "xlsm" }, "application/vnd.ms-excel.sheet.macroenabled.12"),
    XLTM(new String[]{ "xltm" }, "application/vnd.ms-excel.template.macroenabled.12"),
    EOT(new String[]{ "eot" }, "application/vnd.ms-fontobject"),
    CHM(new String[]{ "chm" }, "application/vnd.ms-htmlhelp"),
    IMS(new String[]{ "ims" }, "application/vnd.ms-ims"),
    LRM(new String[]{ "lrm" }, "application/vnd.ms-lrm"),
    THMX(new String[]{ "thmx" }, "application/vnd.ms-officetheme"),
    CAT(new String[]{ "cat" }, "application/vnd.ms-pki.seccat"),
    STL(new String[]{ "stl" }, "application/vnd.ms-pki.stl"),
    PPT(new String[]{ "ppt" }, "application/vnd.ms-powerpoint"),
    PPS(new String[]{ "pps" }, "application/vnd.ms-powerpoint"),
    POT(new String[]{ "pot" }, "application/vnd.ms-powerpoint"),
    PPAM(new String[]{ "ppam" }, "application/vnd.ms-powerpoint.addin.macroenabled.12"),
    PPTM(new String[]{ "pptm" }, "application/vnd.ms-powerpoint.presentation.macroenabled.12"),
    SLDM(new String[]{ "sldm" }, "application/vnd.ms-powerpoint.slide.macroenabled.12"),
    PPSM(new String[]{ "ppsm" }, "application/vnd.ms-powerpoint.slideshow.macroenabled.12"),
    POTM(new String[]{ "potm" }, "application/vnd.ms-powerpoint.template.macroenabled.12"),
    MPP(new String[]{ "mpp" }, "application/vnd.ms-project"),
    MPT(new String[]{ "mpt" }, "application/vnd.ms-project"),
    DOCM(new String[]{ "docm" }, "application/vnd.ms-word.document.macroenabled.12"),
    DOTM(new String[]{ "dotm" }, "application/vnd.ms-word.template.macroenabled.12"),
    WPS(new String[]{ "wps" }, "application/vnd.ms-works"),
    WKS(new String[]{ "wks" }, "application/vnd.ms-works"),
    WCM(new String[]{ "wcm" }, "application/vnd.ms-works"),
    WDB(new String[]{ "wdb" }, "application/vnd.ms-works"),
    WPL(new String[]{ "wpl" }, "application/vnd.ms-wpl"),
    XPS(new String[]{ "xps" }, "application/vnd.ms-xpsdocument"),
    MSEQ(new String[]{ "mseq" }, "application/vnd.mseq"),
    MUS(new String[]{ "mus" }, "application/vnd.musician"),
    MSTY(new String[]{ "msty" }, "application/vnd.muvee.style"),
    TAGLET(new String[]{ "taglet" }, "application/vnd.mynfc"),
    NLU(new String[]{ "nlu" }, "application/vnd.neurolanguage.nlu"),
    NTF(new String[]{ "ntf" }, "application/vnd.nitf"),
    NITF(new String[]{ "nitf" }, "application/vnd.nitf"),
    NND(new String[]{ "nnd" }, "application/vnd.noblenet-directory"),
    NNS(new String[]{ "nns" }, "application/vnd.noblenet-sealer"),
    NNW(new String[]{ "nnw" }, "application/vnd.noblenet-web"),
    NGDAT(new String[]{ "ngdat" }, "application/vnd.nokia.n-gage.data"),
    N_GAGE(new String[]{ "n-gage" }, "application/vnd.nokia.n-gage.symbian.install"),
    RPST(new String[]{ "rpst" }, "application/vnd.nokia.radio-preset"),
    RPSS(new String[]{ "rpss" }, "application/vnd.nokia.radio-presets"),
    EDM(new String[]{ "edm" }, "application/vnd.novadigm.edm"),
    EDX(new String[]{ "edx" }, "application/vnd.novadigm.edx"),
    EXT(new String[]{ "ext" }, "application/vnd.novadigm.ext"),
    ODC(new String[]{ "odc" }, "application/vnd.oasis.opendocument.chart"),
    OTC(new String[]{ "otc" }, "application/vnd.oasis.opendocument.chart-template"),
    ODB(new String[]{ "odb" }, "application/vnd.oasis.opendocument.database"),
    ODF(new String[]{ "odf" }, "application/vnd.oasis.opendocument.formula"),
    ODFT(new String[]{ "odft" }, "application/vnd.oasis.opendocument.formula-template"),
    ODG(new String[]{ "odg" }, "application/vnd.oasis.opendocument.graphics"),
    OTG(new String[]{ "otg" }, "application/vnd.oasis.opendocument.graphics-template"),
    ODI(new String[]{ "odi" }, "application/vnd.oasis.opendocument.image"),
    OTI(new String[]{ "oti" }, "application/vnd.oasis.opendocument.image-template"),
    ODP(new String[]{ "odp" }, "application/vnd.oasis.opendocument.presentation"),
    OTP(new String[]{ "otp" }, "application/vnd.oasis.opendocument.presentation-template"),
    ODS(new String[]{ "ods" }, "application/vnd.oasis.opendocument.spreadsheet"),
    OTS(new String[]{ "ots" }, "application/vnd.oasis.opendocument.spreadsheet-template"),
    ODT(new String[]{ "odt" }, "application/vnd.oasis.opendocument.text"),
    ODM(new String[]{ "odm" }, "application/vnd.oasis.opendocument.text-master"),
    OTT(new String[]{ "ott" }, "application/vnd.oasis.opendocument.text-template"),
    OTH(new String[]{ "oth" }, "application/vnd.oasis.opendocument.text-web"),
    XO(new String[]{ "xo" }, "application/vnd.olpc-sugar"),
    DD2(new String[]{ "dd2" }, "application/vnd.oma.dd2+xml"),
    OXT(new String[]{ "oxt" }, "application/vnd.openofficeorg.extension"),
    PPTX(new String[]{ "pptx" }, "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    SLDX(new String[]{ "sldx" }, "application/vnd.openxmlformats-officedocument.presentationml.slide"),
    PPSX(new String[]{ "ppsx" }, "application/vnd.openxmlformats-officedocument.presentationml.slideshow"),
    POTX(new String[]{ "potx" }, "application/vnd.openxmlformats-officedocument.presentationml.template"),
    XLSX(new String[]{ "xlsx" }, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLTX(new String[]{ "xltx" }, "application/vnd.openxmlformats-officedocument.spreadsheetml.template"),
    DOCX(new String[]{ "docx" }, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOTX(new String[]{ "dotx" }, "application/vnd.openxmlformats-officedocument.wordprocessingml.template"),
    MGP(new String[]{ "mgp" }, "application/vnd.osgeo.mapguide.package"),
    DP(new String[]{ "dp" }, "application/vnd.osgi.dp"),
    ESA(new String[]{ "esa" }, "application/vnd.osgi.subsystem"),
    PDB(new String[]{ "pdb" }, "application/vnd.palm"),
    PQA(new String[]{ "pqa" }, "application/vnd.palm"),
    OPRC(new String[]{ "oprc" }, "application/vnd.palm"),
    PAW(new String[]{ "paw" }, "application/vnd.pawaafile"),
    STR(new String[]{ "str" }, "application/vnd.pg.format"),
    EI6(new String[]{ "ei6" }, "application/vnd.pg.osasli"),
    EFIF(new String[]{ "efif" }, "application/vnd.picsel"),
    WG(new String[]{ "wg" }, "application/vnd.pmi.widget"),
    PLF(new String[]{ "plf" }, "application/vnd.pocketlearn"),
    PBD(new String[]{ "pbd" }, "application/vnd.powerbuilder6"),
    BOX(new String[]{ "box" }, "application/vnd.previewsystems.box"),
    MGZ(new String[]{ "mgz" }, "application/vnd.proteus.magazine"),
    QPS(new String[]{ "qps" }, "application/vnd.publishare-delta-tree"),
    PTID(new String[]{ "ptid" }, "application/vnd.pvi.ptid1"),
    QXD(new String[]{ "qxd" }, "application/vnd.quark.quarkxpress"),
    QXT(new String[]{ "qxt" }, "application/vnd.quark.quarkxpress"),
    QWD(new String[]{ "qwd" }, "application/vnd.quark.quarkxpress"),
    QWT(new String[]{ "qwt" }, "application/vnd.quark.quarkxpress"),
    QXL(new String[]{ "qxl" }, "application/vnd.quark.quarkxpress"),
    QXB(new String[]{ "qxb" }, "application/vnd.quark.quarkxpress"),
    BED(new String[]{ "bed" }, "application/vnd.realvnc.bed"),
    MXL(new String[]{ "mxl" }, "application/vnd.recordare.musicxml"),
    MUSICXML(new String[]{ "musicxml" }, "application/vnd.recordare.musicxml+xml"),
    CRYPTONOTE(new String[]{ "cryptonote" }, "application/vnd.rig.cryptonote"),
    COD(new String[]{ "cod" }, "application/vnd.rim.cod"),
    RM(new String[]{ "rm" }, "application/vnd.rn-realmedia"),
    RMVB(new String[]{ "rmvb" }, "application/vnd.rn-realmedia-vbr"),
    LINK66(new String[]{ "link66" }, "application/vnd.route66.link66+xml"),
    ST(new String[]{ "st" }, "application/vnd.sailingtracker.track"),
    SEE(new String[]{ "see" }, "application/vnd.seemail"),
    SEMA(new String[]{ "sema" }, "application/vnd.sema"),
    SEMD(new String[]{ "semd" }, "application/vnd.semd"),
    SEMF(new String[]{ "semf" }, "application/vnd.semf"),
    IFM(new String[]{ "ifm" }, "application/vnd.shana.informed.formdata"),
    ITP(new String[]{ "itp" }, "application/vnd.shana.informed.formtemplate"),
    IIF(new String[]{ "iif" }, "application/vnd.shana.informed.interchange"),
    IPK(new String[]{ "ipk" }, "application/vnd.shana.informed.package"),
    TWD(new String[]{ "twd" }, "application/vnd.simtech-mindmapper"),
    TWDS(new String[]{ "twds" }, "application/vnd.simtech-mindmapper"),
    MMF(new String[]{ "mmf" }, "application/vnd.smaf"),
    TEACHER(new String[]{ "teacher" }, "application/vnd.smart.teacher"),
    SDKM(new String[]{ "sdkm" }, "application/vnd.solent.sdkm+xml"),
    SDKD(new String[]{ "sdkd" }, "application/vnd.solent.sdkm+xml"),
    DXP(new String[]{ "dxp" }, "application/vnd.spotfire.dxp"),
    SFS(new String[]{ "sfs" }, "application/vnd.spotfire.sfs"),
    SDC(new String[]{ "sdc" }, "application/vnd.stardivision.calc"),
    SDA(new String[]{ "sda" }, "application/vnd.stardivision.draw"),
    SDD(new String[]{ "sdd" }, "application/vnd.stardivision.impress"),
    SMF(new String[]{ "smf" }, "application/vnd.stardivision.math"),
    SDW(new String[]{ "sdw" }, "application/vnd.stardivision.writer"),
    VOR(new String[]{ "vor" }, "application/vnd.stardivision.writer"),
    SGL(new String[]{ "sgl" }, "application/vnd.stardivision.writer-global"),
    SMZIP(new String[]{ "smzip" }, "application/vnd.stepmania.package"),
    SM(new String[]{ "sm" }, "application/vnd.stepmania.stepchart"),
    SXC(new String[]{ "sxc" }, "application/vnd.sun.xml.calc"),
    STC(new String[]{ "stc" }, "application/vnd.sun.xml.calc.template"),
    SXD(new String[]{ "sxd" }, "application/vnd.sun.xml.draw"),
    STD(new String[]{ "std" }, "application/vnd.sun.xml.draw.template"),
    SXI(new String[]{ "sxi" }, "application/vnd.sun.xml.impress"),
    STI(new String[]{ "sti" }, "application/vnd.sun.xml.impress.template"),
    SXM(new String[]{ "sxm" }, "application/vnd.sun.xml.math"),
    SXW(new String[]{ "sxw" }, "application/vnd.sun.xml.writer"),
    SXG(new String[]{ "sxg" }, "application/vnd.sun.xml.writer.global"),
    STW(new String[]{ "stw" }, "application/vnd.sun.xml.writer.template"),
    SUS(new String[]{ "sus" }, "application/vnd.sus-calendar"),
    SUSP(new String[]{ "susp" }, "application/vnd.sus-calendar"),
    SVD(new String[]{ "svd" }, "application/vnd.svd"),
    SIS(new String[]{ "sis" }, "application/vnd.symbian.install"),
    SISX(new String[]{ "sisx" }, "application/vnd.symbian.install"),
    XSM(new String[]{ "xsm" }, "application/vnd.syncml+xml"),
    BDM(new String[]{ "bdm" }, "application/vnd.syncml.dm+wbxml"),
    XDM(new String[]{ "xdm" }, "application/vnd.syncml.dm+xml"),
    TAO(new String[]{ "tao" }, "application/vnd.tao.intent-module-archive"),
    PCAP(new String[]{ "pcap" }, "application/vnd.tcpdump.pcap"),
    CAP(new String[]{ "cap" }, "application/vnd.tcpdump.pcap"),
    DMP(new String[]{ "dmp" }, "application/vnd.tcpdump.pcap"),
    TMO(new String[]{ "tmo" }, "application/vnd.tmobile-livetv"),
    TPT(new String[]{ "tpt" }, "application/vnd.trid.tpt"),
    MXS(new String[]{ "mxs" }, "application/vnd.triscape.mxs"),
    TRA(new String[]{ "tra" }, "application/vnd.trueapp"),
    UFD(new String[]{ "ufd" }, "application/vnd.ufdl"),
    UFDL(new String[]{ "ufdl" }, "application/vnd.ufdl"),
    UTZ(new String[]{ "utz" }, "application/vnd.uiq.theme"),
    UMJ(new String[]{ "umj" }, "application/vnd.umajin"),
    UNITYWEB(new String[]{ "unityweb" }, "application/vnd.unity"),
    UOML(new String[]{ "uoml" }, "application/vnd.uoml+xml"),
    VCX(new String[]{ "vcx" }, "application/vnd.vcx"),
    VSD(new String[]{ "vsd" }, "application/vnd.visio"),
    VST(new String[]{ "vst" }, "application/vnd.visio"),
    VSS(new String[]{ "vss" }, "application/vnd.visio"),
    VSW(new String[]{ "vsw" }, "application/vnd.visio"),
    VIS(new String[]{ "vis" }, "application/vnd.visionary"),
    VSF(new String[]{ "vsf" }, "application/vnd.vsf"),
    WBXML(new String[]{ "wbxml" }, "application/vnd.wap.wbxml"),
    WMLC(new String[]{ "wmlc" }, "application/vnd.wap.wmlc"),
    WMLSC(new String[]{ "wmlsc" }, "application/vnd.wap.wmlscriptc"),
    WTB(new String[]{ "wtb" }, "application/vnd.webturbo"),
    NBP(new String[]{ "nbp" }, "application/vnd.wolfram.player"),
    WPD(new String[]{ "wpd" }, "application/vnd.wordperfect"),
    WQD(new String[]{ "wqd" }, "application/vnd.wqd"),
    STF(new String[]{ "stf" }, "application/vnd.wt.stf"),
    XAR(new String[]{ "xar" }, "application/vnd.xara"),
    XFDL(new String[]{ "xfdl" }, "application/vnd.xfdl"),
    HVD(new String[]{ "hvd" }, "application/vnd.yamaha.hv-dic"),
    HVS(new String[]{ "hvs" }, "application/vnd.yamaha.hv-script"),
    HVP(new String[]{ "hvp" }, "application/vnd.yamaha.hv-voice"),
    OSF(new String[]{ "osf" }, "application/vnd.yamaha.openscoreformat"),
    OSFPVG(new String[]{ "osfpvg" }, "application/vnd.yamaha.openscoreformat.osfpvg+xml"),
    SAF(new String[]{ "saf" }, "application/vnd.yamaha.smaf-audio"),
    SPF(new String[]{ "spf" }, "application/vnd.yamaha.smaf-phrase"),
    CMP(new String[]{ "cmp" }, "application/vnd.yellowriver-custom-menu"),
    ZIR(new String[]{ "zir" }, "application/vnd.zul"),
    ZIRZ(new String[]{ "zirz" }, "application/vnd.zul"),
    ZAZ(new String[]{ "zaz" }, "application/vnd.zzazz.deck+xml"),
    VXML(new String[]{ "vxml" }, "application/voicexml+xml"),
    WGT(new String[]{ "wgt" }, "application/widget"),
    HLP(new String[]{ "hlp" }, "application/winhlp"),
    WSDL(new String[]{ "wsdl" }, "application/wsdl+xml"),
    WSPOLICY(new String[]{ "wspolicy" }, "application/wspolicy+xml"),
    _7Z(new String[]{ "7z" }, "application/x-7z-compressed"),
    ABW(new String[]{ "abw" }, "application/x-abiword"),
    ACE(new String[]{ "ace" }, "application/x-ace-compressed"),
    DMG(new String[]{ "dmg" }, "application/x-apple-diskimage"),
    AAB(new String[]{ "aab" }, "application/x-authorware-bin"),
    X32(new String[]{ "x32" }, "application/x-authorware-bin"),
    U32(new String[]{ "u32" }, "application/x-authorware-bin"),
    VOX(new String[]{ "vox" }, "application/x-authorware-bin"),
    AAM(new String[]{ "aam" }, "application/x-authorware-map"),
    AAS(new String[]{ "aas" }, "application/x-authorware-seg"),
    BCPIO(new String[]{ "bcpio" }, "application/x-bcpio"),
    TORRENT(new String[]{ "torrent" }, "application/x-bittorrent"),
    BLB(new String[]{ "blb" }, "application/x-blorb"),
    BLORB(new String[]{ "blorb" }, "application/x-blorb"),
    BZ2(new String[]{ "bz2" }, "application/x-bzip2"),
    BOZ(new String[]{ "boz" }, "application/x-bzip2"),
    CBR(new String[]{ "cbr" }, "application/x-cbr"),
    CBA(new String[]{ "cba" }, "application/x-cbr"),
    CBT(new String[]{ "cbt" }, "application/x-cbr"),
    CBZ(new String[]{ "cbz" }, "application/x-cbr"),
    CB7(new String[]{ "cb7" }, "application/x-cbr"),
    VCD(new String[]{ "vcd" }, "application/x-cdlink"),
    CFS(new String[]{ "cfs" }, "application/x-cfs-compressed"),
    CHAT(new String[]{ "chat" }, "application/x-chat"),
    PGN(new String[]{ "pgn" }, "application/x-chess-pgn"),
    NSC(new String[]{ "nsc" }, "application/x-conference"),
    CPIO(new String[]{ "cpio" }, "application/x-cpio"),
    CSH(new String[]{ "csh" }, "application/x-csh"),
    DEB(new String[]{ "deb" }, "application/x-debian-package"),
    UDEB(new String[]{ "udeb" }, "application/x-debian-package"),
    DGC(new String[]{ "dgc" }, "application/x-dgc-compressed"),
    DIR(new String[]{ "dir" }, "application/x-director"),
    DCR(new String[]{ "dcr" }, "application/x-director"),
    DXR(new String[]{ "dxr" }, "application/x-director"),
    CST(new String[]{ "cst" }, "application/x-director"),
    CCT(new String[]{ "cct" }, "application/x-director"),
    CXT(new String[]{ "cxt" }, "application/x-director"),
    W3D(new String[]{ "w3d" }, "application/x-director"),
    FGD(new String[]{ "fgd" }, "application/x-director"),
    SWA(new String[]{ "swa" }, "application/x-director"),
    WAD(new String[]{ "wad" }, "application/x-doom"),
    NCX(new String[]{ "ncx" }, "application/x-dtbncx+xml"),
    DTB(new String[]{ "dtb" }, "application/x-dtbook+xml"),
    RES(new String[]{ "res" }, "application/x-dtbresource+xml"),
    DVI(new String[]{ "dvi" }, "application/x-dvi"),
    EVY(new String[]{ "evy" }, "application/x-envoy"),
    EVA(new String[]{ "eva" }, "application/x-eva"),
    BDF(new String[]{ "bdf" }, "application/x-font-bdf"),
    GSF(new String[]{ "gsf" }, "application/x-font-ghostscript"),
    PSF(new String[]{ "psf" }, "application/x-font-linux-psf"),
    PCF(new String[]{ "pcf" }, "application/x-font-pcf"),
    SNF(new String[]{ "snf" }, "application/x-font-snf"),
    PFA(new String[]{ "pfa" }, "application/x-font-type1"),
    PFB(new String[]{ "pfb" }, "application/x-font-type1"),
    PFM(new String[]{ "pfm" }, "application/x-font-type1"),
    AFM(new String[]{ "afm" }, "application/x-font-type1"),
    ARC(new String[]{ "arc" }, "application/x-freearc"),
    SPL(new String[]{ "spl" }, "application/x-futuresplash"),
    GCA(new String[]{ "gca" }, "application/x-gca-compressed"),
    ULX(new String[]{ "ulx" }, "application/x-glulx"),
    GNUMERIC(new String[]{ "gnumeric" }, "application/x-gnumeric"),
    GRAMPS(new String[]{ "gramps" }, "application/x-gramps-xml"),
    GTAR(new String[]{ "gtar" }, "application/x-gtar"),
    HDF(new String[]{ "hdf" }, "application/x-hdf"),
    INSTALL(new String[]{ "install" }, "application/x-install-instructions"),
    ISO(new String[]{ "iso" }, "application/x-iso9660-image"),
    JNLP(new String[]{ "jnlp" }, "application/x-java-jnlp-file"),
    LATEX(new String[]{ "latex" }, "application/x-latex"),
    LZH(new String[]{ "lzh" }, "application/x-lzh-compressed"),
    LHA(new String[]{ "lha" }, "application/x-lzh-compressed"),
    MIE(new String[]{ "mie" }, "application/x-mie"),
    PRC(new String[]{ "prc" }, "application/x-mobipocket-ebook"),
    MOBI(new String[]{ "mobi" }, "application/x-mobipocket-ebook"),
    APPLICATION(new String[]{ "application" }, "application/x-ms-application"),
    LNK(new String[]{ "lnk" }, "application/x-ms-shortcut"),
    WMD(new String[]{ "wmd" }, "application/x-ms-wmd"),
    WMZ(new String[]{ "wmz" }, "application/x-ms-wmz"),
    XBAP(new String[]{ "xbap" }, "application/x-ms-xbap"),
    MDB(new String[]{ "mdb" }, "application/x-msaccess"),
    OBD(new String[]{ "obd" }, "application/x-msbinder"),
    CRD(new String[]{ "crd" }, "application/x-mscardfile"),
    CLP(new String[]{ "clp" }, "application/x-msclip"),
    MVB(new String[]{ "mvb" }, "application/x-msmediaview"),
    M13(new String[]{ "m13" }, "application/x-msmediaview"),
    M14(new String[]{ "m14" }, "application/x-msmediaview"),
    WMF(new String[]{ "wmf" }, "application/x-msmetafile"),
    EMF(new String[]{ "emf" }, "application/x-msmetafile"),
    EMZ(new String[]{ "emz" }, "application/x-msmetafile"),
    MNY(new String[]{ "mny" }, "application/x-msmoney"),
    PUB(new String[]{ "pub" }, "application/x-mspublisher"),
    SCD(new String[]{ "scd" }, "application/x-msschedule"),
    TRM(new String[]{ "trm" }, "application/x-msterminal"),
    WRI(new String[]{ "wri" }, "application/x-mswrite"),
    NC(new String[]{ "nc" }, "application/x-netcdf"),
    CDF(new String[]{ "cdf" }, "application/x-netcdf"),
    NZB(new String[]{ "nzb" }, "application/x-nzb"),
    P12(new String[]{ "p12" }, "application/x-pkcs12"),
    PFX(new String[]{ "pfx" }, "application/x-pkcs12"),
    P7B(new String[]{ "p7b" }, "application/x-pkcs7-certificates"),
    SPC(new String[]{ "spc" }, "application/x-pkcs7-certificates"),
    P7R(new String[]{ "p7r" }, "application/x-pkcs7-certreqresp"),
    RAR(new String[]{ "rar" }, "application/x-rar-compressed"),
    RIS(new String[]{ "ris" }, "application/x-research-info-systems"),
    SH(new String[]{ "sh" }, "application/x-sh"),
    SHAR(new String[]{ "shar" }, "application/x-shar"),
    SWF(new String[]{ "swf" }, "application/x-shockwave-flash"),
    XAP(new String[]{ "xap" }, "application/x-silverlight-app"),
    SQL(new String[]{ "sql" }, "application/x-sql"),
    SIT(new String[]{ "sit" }, "application/x-stuffit"),
    SITX(new String[]{ "sitx" }, "application/x-stuffitx"),
    SRT(new String[]{ "srt" }, "application/x-subrip"),
    SV4CPIO(new String[]{ "sv4cpio" }, "application/x-sv4cpio"),
    SV4CRC(new String[]{ "sv4crc" }, "application/x-sv4crc"),
    T3(new String[]{ "t3" }, "application/x-t3vm-image"),
    GAM(new String[]{ "gam" }, "application/x-tads"),
    TCL(new String[]{ "tcl" }, "application/x-tcl"),
    TEX(new String[]{ "tex" }, "application/x-tex"),
    TFM(new String[]{ "tfm" }, "application/x-tex-tfm"),
    TEXINFO(new String[]{ "texinfo" }, "application/x-texinfo"),
    TEXI(new String[]{ "texi" }, "application/x-texinfo"),
    OBJ(new String[]{ "obj" }, "application/x-tgif"),
    USTAR(new String[]{ "ustar" }, "application/x-ustar"),
    SRC(new String[]{ "src" }, "application/x-wais-source"),
    DER(new String[]{ "der" }, "application/x-x509-ca-cert"),
    CRT(new String[]{ "crt" }, "application/x-x509-ca-cert"),
    XLF(new String[]{ "xlf" }, "application/x-xliff+xml"),
    XPI(new String[]{ "xpi" }, "application/x-xpinstall"),
    XZ(new String[]{ "xz" }, "application/x-xz"),
    Z1(new String[]{ "z1" }, "application/x-zmachine"),
    Z2(new String[]{ "z2" }, "application/x-zmachine"),
    Z3(new String[]{ "z3" }, "application/x-zmachine"),
    Z4(new String[]{ "z4" }, "application/x-zmachine"),
    Z5(new String[]{ "z5" }, "application/x-zmachine"),
    Z6(new String[]{ "z6" }, "application/x-zmachine"),
    Z7(new String[]{ "z7" }, "application/x-zmachine"),
    Z8(new String[]{ "z8" }, "application/x-zmachine"),
    XAML(new String[]{ "xaml" }, "application/xaml+xml"),
    XDF(new String[]{ "xdf" }, "application/xcap-diff+xml"),
    XENC(new String[]{ "xenc" }, "application/xenc+xml"),
    XHTML(new String[]{ "xhtml" }, "application/xhtml+xml"),
    XHT(new String[]{ "xht" }, "application/xhtml+xml"),
    XML(new String[]{ "xml" }, "application/xml"),
    XSL(new String[]{ "xsl" }, "application/xml"),
    DTD(new String[]{ "dtd" }, "application/xml-dtd"),
    XOP(new String[]{ "xop" }, "application/xop+xml"),
    XPL(new String[]{ "xpl" }, "application/xproc+xml"),
    XSLT(new String[]{ "xslt" }, "application/xslt+xml"),
    XSPF(new String[]{ "xspf" }, "application/xspf+xml"),
    MXML(new String[]{ "mxml" }, "application/xv+xml"),
    XHVML(new String[]{ "xhvml" }, "application/xv+xml"),
    XVML(new String[]{ "xvml" }, "application/xv+xml"),
    XVM(new String[]{ "xvm" }, "application/xv+xml"),
    YANG(new String[]{ "yang" }, "application/yang"),
    YIN(new String[]{ "yin" }, "application/yin+xml"),
    ADP(new String[]{ "adp" }, "audio/adpcm"),
    AU(new String[]{ "au" }, "audio/basic"),
    SND(new String[]{ "snd" }, "audio/basic"),
    MID(new String[]{ "mid" }, "audio/midi"),
    MIDI(new String[]{ "midi" }, "audio/midi"),
    KAR(new String[]{ "kar" }, "audio/midi"),
    RMI(new String[]{ "rmi" }, "audio/midi"),
    M4A(new String[]{ "m4a" }, "audio/mp4"),
    MP4A(new String[]{ "mp4a" }, "audio/mp4"),
    MPGA(new String[]{ "mpga" }, "audio/mpeg"),
    MP2(new String[]{ "mp2" }, "audio/mpeg"),
    MP2A(new String[]{ "mp2a" }, "audio/mpeg"),
    MP3(new String[]{ "mp3" }, "audio/mpeg"),
    M2A(new String[]{ "m2a" }, "audio/mpeg"),
    M3A(new String[]{ "m3a" }, "audio/mpeg"),
    OGA(new String[]{ "oga" }, "audio/ogg"),
    OGG(new String[]{ "ogg" }, "audio/ogg"),
    SPX(new String[]{ "spx" }, "audio/ogg"),
    S3M(new String[]{ "s3m" }, "audio/s3m"),
    SIL(new String[]{ "sil" }, "audio/silk"),
    UVA(new String[]{ "uva" }, "audio/vnd.dece.audio"),
    UVVA(new String[]{ "uvva" }, "audio/vnd.dece.audio"),
    EOL(new String[]{ "eol" }, "audio/vnd.digital-winds"),
    DRA(new String[]{ "dra" }, "audio/vnd.dra"),
    DTS(new String[]{ "dts" }, "audio/vnd.dts"),
    DTSHD(new String[]{ "dtshd" }, "audio/vnd.dts.hd"),
    LVP(new String[]{ "lvp" }, "audio/vnd.lucent.voice"),
    PYA(new String[]{ "pya" }, "audio/vnd.ms-playready.media.pya"),
    ECELP4800(new String[]{ "ecelp4800" }, "audio/vnd.nuera.ecelp4800"),
    ECELP7470(new String[]{ "ecelp7470" }, "audio/vnd.nuera.ecelp7470"),
    ECELP9600(new String[]{ "ecelp9600" }, "audio/vnd.nuera.ecelp9600"),
    RIP(new String[]{ "rip" }, "audio/vnd.rip"),
    WEBA(new String[]{ "weba" }, "audio/webm"),
    AAC(new String[]{ "aac" }, "audio/x-aac"),
    AIF(new String[]{ "aif" }, "audio/x-aiff"),
    AIFF(new String[]{ "aiff" }, "audio/x-aiff"),
    AIFC(new String[]{ "aifc" }, "audio/x-aiff"),
    CAF(new String[]{ "caf" }, "audio/x-caf"),
    FLAC(new String[]{ "flac" }, "audio/x-flac"),
    MKA(new String[]{ "mka" }, "audio/x-matroska"),
    M3U(new String[]{ "m3u" }, "audio/x-mpegurl"),
    WAX(new String[]{ "wax" }, "audio/x-ms-wax"),
    WMA(new String[]{ "wma" }, "audio/x-ms-wma"),
    RAM(new String[]{ "ram" }, "audio/x-pn-realaudio"),
    RA(new String[]{ "ra" }, "audio/x-pn-realaudio"),
    RMP(new String[]{ "rmp" }, "audio/x-pn-realaudio-plugin"),
    WAV(new String[]{ "wav" }, "audio/x-wav"),
    XM(new String[]{ "xm" }, "audio/xm"),
    CDX(new String[]{ "cdx" }, "chemical/x-cdx"),
    CIF(new String[]{ "cif" }, "chemical/x-cif"),
    CMDF(new String[]{ "cmdf" }, "chemical/x-cmdf"),
    CML(new String[]{ "cml" }, "chemical/x-cml"),
    CSML(new String[]{ "csml" }, "chemical/x-csml"),
    XYZ(new String[]{ "xyz" }, "chemical/x-xyz"),
    TTC(new String[]{ "ttc" }, "font/collection"),
    OTF(new String[]{ "otf" }, "font/otf"),
    TTF(new String[]{ "ttf" }, "font/ttf"),
    WOFF(new String[]{ "woff" }, "font/woff"),
    WOFF2(new String[]{ "woff2" }, "font/woff2"),
    CGM(new String[]{ "cgm" }, "image/cgm"),
    G3(new String[]{ "g3" }, "image/g3fax"),
    IEF(new String[]{ "ief" }, "image/ief"),
    KTX(new String[]{ "ktx" }, "image/ktx"),
    BTIF(new String[]{ "btif" }, "image/prs.btif"),
    SGI(new String[]{ "sgi" }, "image/sgi"),
    SVG(new String[]{ "svg" }, "image/svg+xml"),
    SVGZ(new String[]{ "svgz" }, "image/svg+xml"),
    PSD(new String[]{ "psd" }, "image/vnd.adobe.photoshop"),
    UVI(new String[]{ "uvi" }, "image/vnd.dece.graphic"),
    UVVI(new String[]{ "uvvi" }, "image/vnd.dece.graphic"),
    UVG(new String[]{ "uvg" }, "image/vnd.dece.graphic"),
    UVVG(new String[]{ "uvvg" }, "image/vnd.dece.graphic"),
    DJVU(new String[]{ "djvu" }, "image/vnd.djvu"),
    DJV(new String[]{ "djv" }, "image/vnd.djvu"),
    DWG(new String[]{ "dwg" }, "image/vnd.dwg"),
    DXF(new String[]{ "dxf" }, "image/vnd.dxf"),
    FBS(new String[]{ "fbs" }, "image/vnd.fastbidsheet"),
    FPX(new String[]{ "fpx" }, "image/vnd.fpx"),
    FST(new String[]{ "fst" }, "image/vnd.fst"),
    MMR(new String[]{ "mmr" }, "image/vnd.fujixerox.edmics-mmr"),
    RLC(new String[]{ "rlc" }, "image/vnd.fujixerox.edmics-rlc"),
    MDI(new String[]{ "mdi" }, "image/vnd.ms-modi"),
    WDP(new String[]{ "wdp" }, "image/vnd.ms-photo"),
    NPX(new String[]{ "npx" }, "image/vnd.net-fpx"),
    WBMP(new String[]{ "wbmp" }, "image/vnd.wap.wbmp"),
    XIF(new String[]{ "xif" }, "image/vnd.xiff"),
    WEBP(new String[]{ "webp" }, "image/webp"),
    _3DS(new String[]{ "3ds" }, "image/x-3ds"),
    RAS(new String[]{ "ras" }, "image/x-cmu-raster"),
    CMX(new String[]{ "cmx" }, "image/x-cmx"),
    FH(new String[]{ "fh" }, "image/x-freehand"),
    FHC(new String[]{ "fhc" }, "image/x-freehand"),
    FH4(new String[]{ "fh4" }, "image/x-freehand"),
    FH5(new String[]{ "fh5" }, "image/x-freehand"),
    FH7(new String[]{ "fh7" }, "image/x-freehand"),
    ICO(new String[]{ "ico" }, "image/x-icon"),
    SID(new String[]{ "sid" }, "image/x-mrsid-image"),
    PCX(new String[]{ "pcx" }, "image/x-pcx"),
    PIC(new String[]{ "pic" }, "image/x-pict"),
    PCT(new String[]{ "pct" }, "image/x-pict"),
    PNM(new String[]{ "pnm" }, "image/x-portable-anymap"),
    PBM(new String[]{ "pbm" }, "image/x-portable-bitmap"),
    PGM(new String[]{ "pgm" }, "image/x-portable-graymap"),
    PPM(new String[]{ "ppm" }, "image/x-portable-pixmap"),
    TGA(new String[]{ "tga" }, "image/x-tga"),
    XBM(new String[]{ "xbm" }, "image/x-xbitmap"),
    XWD(new String[]{ "xwd" }, "image/x-xwindowdump"),
    EML(new String[]{ "eml" }, "message/rfc822"),
    MIME(new String[]{ "mime" }, "message/rfc822"),
    IGS(new String[]{ "igs" }, "model/iges"),
    IGES(new String[]{ "iges" }, "model/iges"),
    MSH(new String[]{ "msh" }, "model/mesh"),
    MESH(new String[]{ "mesh" }, "model/mesh"),
    SILO(new String[]{ "silo" }, "model/mesh"),
    DAE(new String[]{ "dae" }, "model/vnd.collada+xml"),
    DWF(new String[]{ "dwf" }, "model/vnd.dwf"),
    GDL(new String[]{ "gdl" }, "model/vnd.gdl"),
    GTW(new String[]{ "gtw" }, "model/vnd.gtw"),
    MTS(new String[]{ "mts" }, "model/vnd.mts"),
    VTU(new String[]{ "vtu" }, "model/vnd.vtu"),
    WRL(new String[]{ "wrl" }, "model/vrml"),
    VRML(new String[]{ "vrml" }, "model/vrml"),
    X3DB(new String[]{ "x3db" }, "model/x3d+binary"),
    X3DBZ(new String[]{ "x3dbz" }, "model/x3d+binary"),
    X3DV(new String[]{ "x3dv" }, "model/x3d+vrml"),
    X3DVZ(new String[]{ "x3dvz" }, "model/x3d+vrml"),
    X3D(new String[]{ "x3d" }, "model/x3d+xml"),
    X3DZ(new String[]{ "x3dz" }, "model/x3d+xml"),
    APPCACHE(new String[]{ "appcache" }, "text/cache-manifest"),
    ICS(new String[]{ "ics" }, "text/calendar"),
    IFB(new String[]{ "ifb" }, "text/calendar"),
    CSS(new String[]{ "css" }, "text/css"),
    CSV(new String[]{ "csv" }, "text/csv"),
    HTML(new String[]{ "html" }, "text/html"),
    HTM(new String[]{ "htm" }, "text/html"),
    N3(new String[]{ "n3" }, "text/n3"),
    TXT(new String[]{ "txt" }, "text/plain"),
    TEXT(new String[]{ "text" }, "text/plain"),
    CONF(new String[]{ "conf" }, "text/plain"),
    DEF(new String[]{ "def" }, "text/plain"),
    LIST(new String[]{ "list" }, "text/plain"),
    LOG(new String[]{ "log" }, "text/plain"),
    IN(new String[]{ "in" }, "text/plain"),
    DSC(new String[]{ "dsc" }, "text/prs.lines.tag"),
    RTX(new String[]{ "rtx" }, "text/richtext"),
    SGML(new String[]{ "sgml" }, "text/sgml"),
    SGM(new String[]{ "sgm" }, "text/sgml"),
    TSV(new String[]{ "tsv" }, "text/tab-separated-values"),
    T(new String[]{ "t" }, "text/troff"),
    TR(new String[]{ "tr" }, "text/troff"),
    ROFF(new String[]{ "roff" }, "text/troff"),
    MAN(new String[]{ "man" }, "text/troff"),
    ME(new String[]{ "me" }, "text/troff"),
    MS(new String[]{ "ms" }, "text/troff"),
    TTL(new String[]{ "ttl" }, "text/turtle"),
    URI(new String[]{ "uri" }, "text/uri-list"),
    URIS(new String[]{ "uris" }, "text/uri-list"),
    URLS(new String[]{ "urls" }, "text/uri-list"),
    VCARD(new String[]{ "vcard" }, "text/vcard"),
    CURL(new String[]{ "curl" }, "text/vnd.curl"),
    DCURL(new String[]{ "dcurl" }, "text/vnd.curl.dcurl"),
    MCURL(new String[]{ "mcurl" }, "text/vnd.curl.mcurl"),
    SCURL(new String[]{ "scurl" }, "text/vnd.curl.scurl"),
    SUB(new String[]{ "sub" }, "text/vnd.dvb.subtitle"),
    FLY(new String[]{ "fly" }, "text/vnd.fly"),
    FLX(new String[]{ "flx" }, "text/vnd.fmi.flexstor"),
    GV(new String[]{ "gv" }, "text/vnd.graphviz"),
    _3DML(new String[]{ "3dml" }, "text/vnd.in3d.3dml"),
    SPOT(new String[]{ "spot" }, "text/vnd.in3d.spot"),
    JAD(new String[]{ "jad" }, "text/vnd.sun.j2me.app-descriptor"),
    WML(new String[]{ "wml" }, "text/vnd.wap.wml"),
    WMLS(new String[]{ "wmls" }, "text/vnd.wap.wmlscript"),
    S(new String[]{ "s" }, "text/x-asm"),
    ASM(new String[]{ "asm" }, "text/x-asm"),
    C(new String[]{ "c" }, "text/x-c"),
    CC(new String[]{ "cc" }, "text/x-c"),
    CXX(new String[]{ "cxx" }, "text/x-c"),
    CPP(new String[]{ "cpp" }, "text/x-c"),
    H(new String[]{ "h" }, "text/x-c"),
    HH(new String[]{ "hh" }, "text/x-c"),
    DIC(new String[]{ "dic" }, "text/x-c"),
    F(new String[]{ "f" }, "text/x-fortran"),
    FOR(new String[]{ "for" }, "text/x-fortran"),
    F77(new String[]{ "f77" }, "text/x-fortran"),
    F90(new String[]{ "f90" }, "text/x-fortran"),
    JAVA(new String[]{ "java" }, "text/x-java-source"),
    NFO(new String[]{ "nfo" }, "text/x-nfo"),
    OPML(new String[]{ "opml" }, "text/x-opml"),
    P(new String[]{ "p" }, "text/x-pascal"),
    PAS(new String[]{ "pas" }, "text/x-pascal"),
    ETX(new String[]{ "etx" }, "text/x-setext"),
    SFV(new String[]{ "sfv" }, "text/x-sfv"),
    UU(new String[]{ "uu" }, "text/x-uuencode"),
    VCS(new String[]{ "vcs" }, "text/x-vcalendar"),
    VCF(new String[]{ "vcf" }, "text/x-vcard"),
    _3GP(new String[]{ "3gp" }, "video/3gpp"),
    _3G2(new String[]{ "3g2" }, "video/3gpp2"),
    H261(new String[]{ "h261" }, "video/h261"),
    H263(new String[]{ "h263" }, "video/h263"),
    H264(new String[]{ "h264" }, "video/h264"),
    JPGV(new String[]{ "jpgv" }, "video/jpeg"),
    JPM(new String[]{ "jpm" }, "video/jpm"),
    JPGM(new String[]{ "jpgm" }, "video/jpm"),
    MJ2(new String[]{ "mj2" }, "video/mj2"),
    MJP2(new String[]{ "mjp2" }, "video/mj2"),
    MP4(new String[]{ "mp4" }, "video/mp4"),
    MP4V(new String[]{ "mp4v" }, "video/mp4"),
    MPG4(new String[]{ "mpg4" }, "video/mp4"),
    MPEG(new String[]{ "mpeg" }, "video/mpeg"),
    MPG(new String[]{ "mpg" }, "video/mpeg"),
    MPE(new String[]{ "mpe" }, "video/mpeg"),
    M1V(new String[]{ "m1v" }, "video/mpeg"),
    M2V(new String[]{ "m2v" }, "video/mpeg"),
    OGV(new String[]{ "ogv" }, "video/ogg"),
    QT(new String[]{ "qt" }, "video/quicktime"),
    MOV(new String[]{ "mov" }, "video/quicktime"),
    UVH(new String[]{ "uvh" }, "video/vnd.dece.hd"),
    UVVH(new String[]{ "uvvh" }, "video/vnd.dece.hd"),
    UVM(new String[]{ "uvm" }, "video/vnd.dece.mobile"),
    UVVM(new String[]{ "uvvm" }, "video/vnd.dece.mobile"),
    UVP(new String[]{ "uvp" }, "video/vnd.dece.pd"),
    UVVP(new String[]{ "uvvp" }, "video/vnd.dece.pd"),
    UVS(new String[]{ "uvs" }, "video/vnd.dece.sd"),
    UVVS(new String[]{ "uvvs" }, "video/vnd.dece.sd"),
    UVV(new String[]{ "uvv" }, "video/vnd.dece.video"),
    UVVV(new String[]{ "uvvv" }, "video/vnd.dece.video"),
    DVB(new String[]{ "dvb" }, "video/vnd.dvb.file"),
    FVT(new String[]{ "fvt" }, "video/vnd.fvt"),
    MXU(new String[]{ "mxu" }, "video/vnd.mpegurl"),
    M4U(new String[]{ "m4u" }, "video/vnd.mpegurl"),
    PYV(new String[]{ "pyv" }, "video/vnd.ms-playready.media.pyv"),
    UVU(new String[]{ "uvu" }, "video/vnd.uvvu.mp4"),
    UVVU(new String[]{ "uvvu" }, "video/vnd.uvvu.mp4"),
    VIV(new String[]{ "viv" }, "video/vnd.vivo"),
    WEBM(new String[]{ "webm" }, "video/webm"),
    F4V(new String[]{ "f4v" }, "video/x-f4v"),
    FLI(new String[]{ "fli" }, "video/x-fli"),
    FLV(new String[]{ "flv" }, "video/x-flv"),
    M4V(new String[]{ "m4v" }, "video/x-m4v"),
    MKV(new String[]{ "mkv" }, "video/x-matroska"),
    MK3D(new String[]{ "mk3d" }, "video/x-matroska"),
    MKS(new String[]{ "mks" }, "video/x-matroska"),
    MNG(new String[]{ "mng" }, "video/x-mng"),
    ASF(new String[]{ "asf" }, "video/x-ms-asf"),
    ASX(new String[]{ "asx" }, "video/x-ms-asf"),
    VOB(new String[]{ "vob" }, "video/x-ms-vob"),
    WM(new String[]{ "wm" }, "video/x-ms-wm"),
    WMV(new String[]{ "wmv" }, "video/x-ms-wmv"),
    WMX(new String[]{ "wmx" }, "video/x-ms-wmx"),
    WVX(new String[]{ "wvx" }, "video/x-ms-wvx"),
    AVI(new String[]{ "avi" }, "video/x-msvideo"),
    MOVIE(new String[]{ "movie" }, "video/x-sgi-movie"),
    SMV(new String[]{ "smv" }, "video/x-smv"),
    ICE(new String[]{ "ice" }, "x-conference/x-cooltalk"),

    // 以上代码为机器自动生成

    UNKNOWN(new String[]{ "unknown" }, "application/octet-stream");


    /**
     * 扩展名。
     */
    private String[] extensions;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 文件格式魔数。
     */
    private byte[] magicNumber;

    /**
     *
     */
    private static HashMap<String, FileType> extensionsMap = new HashMap<>();

    /**
     * 构造函数。
     *
     * @param extensions 扩展名。
     * @param mimeType MIME 类型。
     */
    FileType(String[] extensions, String mimeType) {
        this(extensions, mimeType, null);
    }

    /**
     * 构造函数。
     *
     * @param extensions 扩展名。
     * @param mimeType MIME 类型。
     * @param magicNumber 文件格式魔数。
     */
    FileType(String[] extensions, String mimeType, byte[] magicNumber) {
        this.extensions = extensions;
        this.mimeType = mimeType;
        this.magicNumber = magicNumber;
    }

    /**
     * 返回该文件类型的首选扩展名。
     *
     * @return 返回该文件类型的首选扩展名。
     */
    public String getPreferredExtension() {
        return this.extensions[0];
    }

    /**
     * 返回文件类型的后缀名。
     *
     * @return 返回文件类型的后缀名。
     */
    public String[] getExtensions() {
        return this.extensions;
    }

    /**
     * 返回文件类型的 MIME 类型。
     *
     * @return 返回文件类型的 MIME 类型。
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * 返回文件格式的魔数。
     *
     * @return 返回文件格式的魔数。
     */
    public byte[] getMagicNumber() {
        return this.magicNumber;
    }

    /**
     * 通过后缀名匹配文件类型。
     *
     * @param extension
     * @return
     */
    public static FileType matchExtension(String extension) {
        if (null == extension) {
            return UNKNOWN;
        }

        if (extension.equalsIgnoreCase("ignore")) {
            return FILE;
        }

        String lowerCase = extension.toLowerCase();

        FileType fileType = FileType.extensionsMap.get(lowerCase);
        if (null != fileType) {
            return fileType;
        }

        for (FileType type : FileType.values()) {
            for (String ext : type.extensions) {
                if (ext.equals(lowerCase)) {
                    FileType.extensionsMap.put(lowerCase, type);
                    return type;
                }
            }
        }

        return UNKNOWN;
    }

    /**
     * 根据文件魔数提取文件类型。
     *
     * @param data
     * @return
     */
    public static FileType extractFileType(byte[] data) {
        if (data.length < 4) {
            return UNKNOWN;
        }

        for (FileType type : FileType.values()) {
            if (null == type.magicNumber) {
                continue;
            }

            boolean matching = true;

            byte[] mn = type.magicNumber;
            for (int i = 0; i < mn.length && i < data.length; ++i) {
                if (mn[i] != data[i]) {
                    matching = false;
                    break;
                }
            }

            if (matching) {
                return type;
            }
        }

        return UNKNOWN;
    }

    /* 仅用于代码维护
    private static void makeCode() {
        File src = new File("/Users/ambrose/Documents/Repositories/Cube3/others/mime.types");

        StringBuilder buf = new StringBuilder();

        ArrayList<String> extensions = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(src));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                String mime = null;
                extensions.clear();

                String[] tmp = line.split("\t");

                mime = tmp[0].trim();

                for (int i = 1; i < tmp.length; ++i) {
                    String s = tmp[i];
                    if (s.length() == 0) {
                        continue;
                    }

                    String[] exts = s.split(" ");
                    for (String ext : exts) {
                        if (FileType.UNKNOWN != FileType.parse(ext)) {
                            break;
                        }

                        extensions.add(ext);
                    }
                }

                if (!extensions.isEmpty()) {
                    for (int i = 0; i < extensions.size(); ++i) {
                        String ext = extensions.get(i);

                        String name = ext.toUpperCase();
                        name = name.replaceAll("-", "_");

                        if (name.startsWith("1") || name.startsWith("3") || name.startsWith("7")) {
                            name = "_" + name;
                        }

                        buf.append("\t").append(name).append("(");
                        buf.append("new String[]{ \"").append(ext).append("\" }");
                        buf.append(", ");
                        buf.append("\"").append(mime).append("\"");
                        buf.append("),\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(buf.toString());

        Path path = Paths.get("/Users/ambrose/Documents/Repositories/Cube3/others/mine.txt");
        try {
            Files.write(path, buf.toString().getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */
}
