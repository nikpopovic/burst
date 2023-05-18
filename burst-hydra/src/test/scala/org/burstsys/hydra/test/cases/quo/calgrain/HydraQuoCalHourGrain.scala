/* Copyright Yahoo, Licensed under the terms of the Apache 2.0 license. See LICENSE file in project root for terms. */
package org.burstsys.hydra.test.cases.quo.calgrain

import org.burstsys.fabric.wave.execution.model.result.group.FabricResultGroup
import org.burstsys.fabric.wave.execution.model.result.row.FabricResultRow
import org.burstsys.hydra.test.cases.quo.calordinals.HydraQuoCalDayOfYearOrdinal.{analysisName, frameName}
import org.burstsys.hydra.test.cases.support.HydraUseCase

object HydraQuoCalHourGrain extends HydraUseCase(1, 1, "quo") {
  // override val serializeTraversal = true

  override def analysisSource: String =
    s"""
       |hydra $analysisName() {
       |  schema quo
       |  frame $frameName  {
       |    cube user {
       |      limit = 9999
       |      cube user.sessions.events {
       |        aggregates {
       |          count:sum[long]
       |        }
       |        dimensions {
       |          grain:hourGrain[long]
       |        }
       |      }
       |    }
       |
       |    user.sessions.events.parameters => {
       |      situ => {
       |        $analysisName.$frameName.grain = user.sessions.events.startTime
       |        $analysisName.$frameName.count = 1
       |      }
       |    }
       |
       |  }
       |}
     """.stripMargin

  override def
  validate(implicit result: FabricResultGroup): Unit = {
    val r = result.resultSets(result.resultSetNames(frameName))
    assertLimits(r)
    found(r.rowSet) should equal(expected)
  }


  def found(rowSet: Array[FabricResultRow]): Array[_] = {
    rowSet.map {
      row => (row.cells(0).asLong, row.cells(1).asLong)
    }.sortBy(_._1)
  }

  val expected: Array[Any] =
    Array((1419321600000L,498), (1419325200000L,468), (1419328800000L,933), (1419332400000L,257), (1419336000000L,55), (1419343200000L,8), (1419350400000L,2706), (1419354000000L,1087), (1419357600000L,2350), (1419361200000L,6), (1419364800000L,3730), (1419368400000L,3395), (1419372000000L,4849), (1419375600000L,2809), (1419397200000L,950), (1419400800000L,1143), (1419408000000L,6), (1419411600000L,172), (1419426000000L,48), (1419429600000L,11), (1419436800000L,1072), (1419440400000L,3629), (1419444000000L,3669), (1419447600000L,3803), (1419451200000L,885), (1419454800000L,6182), (1419458400000L,1478), (1419469200000L,6), (1419483600000L,10), (1419487200000L,174), (1419494400000L,183), (1419505200000L,342), (1419523200000L,1106), (1419526800000L,204), (1419534000000L,3197), (1419537600000L,3279), (1419541200000L,520), (1419544800000L,1779), (1419548400000L,5028), (1419552000000L,133), (1419555600000L,35), (1419559200000L,538), (1419570000000L,416), (1419573600000L,10), (1419577200000L,103), (1419584400000L,413), (1419588000000L,6), (1419591600000L,390), (1419598800000L,6), (1419602400000L,27), (1419606000000L,1527), (1419609600000L,5906), (1419613200000L,4819), (1419616800000L,4821), (1419620400000L,876), (1419624000000L,3720), (1419627600000L,6941), (1419631200000L,3218), (1419634800000L,871), (1419638400000L,343), (1419642000000L,427), (1419652800000L,8), (1419656400000L,1690), (1419663600000L,9), (1419674400000L,20), (1419678000000L,329), (1419696000000L,344), (1419699600000L,539), (1419703200000L,5356), (1419706800000L,517), (1419710400000L,3303), (1419714000000L,3189), (1419717600000L,1703), (1419721200000L,3402), (1419724800000L,3899), (1419728400000L,618), (1419732000000L,3199), (1419735600000L,749), (1419739200000L,2), (1419742800000L,243), (1419750000000L,1834), (1419760800000L,514), (1419768000000L,6), (1419778800000L,2233), (1419782400000L,1265), (1419786000000L,251), (1419789600000L,717), (1419793200000L,1916), (1419796800000L,2086), (1419800400000L,3515), (1419804000000L,2354), (1419807600000L,5619), (1419811200000L,2847), (1419818400000L,2359), (1419825600000L,1622), (1419829200000L,628), (1419832800000L,562), (1419840000000L,34), (1419843600000L,24), (1419847200000L,465), (1419865200000L,8), (1419868800000L,461), (1419876000000L,869), (1419879600000L,505), (1419883200000L,2651), (1419890400000L,6), (1419894000000L,301), (1419897600000L,358), (1419901200000L,42), (1419912000000L,53), (1419915600000L,552), (1419919200000L,4), (1419937200000L,76), (1419948000000L,1322), (1419955200000L,10), (1419962400000L,393), (1419966000000L,4), (1419969600000L,6), (1419984000000L,557), (1419987600000L,81), (1419991200000L,629), (1420002000000L,324), (1420005600000L,1373), (1420009200000L,55), (1420023600000L,554), (1420027200000L,25), (1420034400000L,6), (1420038000000L,65), (1420045200000L,2), (1420048800000L,66), (1420052400000L,12), (1420056000000L,2041), (1420059600000L,3805), (1420063200000L,3900), (1420066800000L,1449), (1420070400000L,71), (1420074000000L,4), (1420077600000L,6), (1420081200000L,34), (1420084800000L,8), (1420092000000L,940), (1420095600000L,2), (1420099200000L,187), (1420102800000L,88), (1420106400000L,25), (1420117200000L,194), (1420124400000L,964), (1420128000000L,525), (1420131600000L,1460), (1420135200000L,3133), (1420138800000L,1717), (1420142400000L,2790), (1420146000000L,4133), (1420149600000L,3586), (1420153200000L,5836), (1420156800000L,4188), (1420160400000L,3276), (1420164000000L,1670), (1420167600000L,579), (1420171200000L,335), (1420174800000L,1415), (1420178400000L,300), (1420182000000L,265), (1420185600000L,10), (1420189200000L,2111), (1420192800000L,59), (1420196400000L,624), (1420200000000L,130), (1420203600000L,3303), (1420207200000L,3966), (1420210800000L,2966), (1420214400000L,98), (1420218000000L,136), (1420221600000L,461), (1420225200000L,154), (1420228800000L,2040), (1420232400000L,3543), (1420236000000L,4000), (1420239600000L,3503), (1420243200000L,343), (1420246800000L,202), (1420250400000L,1490), (1420254000000L,2472), (1420261200000L,502), (1420264800000L,28), (1420268400000L,246), (1420272000000L,467), (1420275600000L,296), (1420279200000L,4), (1420286400000L,86), (1420290000000L,6), (1420293600000L,611), (1420300800000L,10), (1420304400000L,365), (1420308000000L,3170), (1420311600000L,2472), (1420315200000L,4868), (1420318800000L,5130), (1420322400000L,827), (1420326000000L,82), (1420329600000L,1905), (1420333200000L,3447), (1420336800000L,4463), (1420340400000L,111), (1420347600000L,3177), (1420351200000L,507), (1420354800000L,916), (1420362000000L,18), (1420365600000L,9), (1420369200000L,215), (1420372800000L,299), (1420376400000L,6), (1420380000000L,24), (1420383600000L,204), (1420387200000L,919), (1420390800000L,3742), (1420394400000L,1592), (1420398000000L,3508), (1420401600000L,114), (1420405200000L,447), (1420408800000L,4238), (1420412400000L,489), (1420416000000L,320), (1420434000000L,2404), (1420437600000L,1515), (1420441200000L,87), (1420444800000L,36), (1420452000000L,1928), (1420455600000L,34), (1420459200000L,2), (1420462800000L,153), (1420466400000L,126), (1420470000000L,300), (1420477200000L,39), (1420480800000L,353), (1420484400000L,1347), (1420495200000L,111), (1420498800000L,840), (1420502400000L,1309), (1420509600000L,2115), (1420513200000L,995), (1420520400000L,8), (1420531200000L,6), (1420534800000L,440), (1420538400000L,75), (1420542000000L,69), (1420545600000L,500), (1420549200000L,1513), (1420552800000L,255), (1420560000000L,159), (1420563600000L,62), (1420567200000L,1195), (1420574400000L,1138), (1420581600000L,1558), (1420585200000L,2), (1420588800000L,608), (1420599600000L,6), (1420603200000L,1599), (1420606800000L,430), (1420610400000L,78), (1420614000000L,238), (1420632000000L,293), (1420639200000L,10), (1420642800000L,225), (1420646400000L,924), (1420650000000L,85), (1420653600000L,345), (1420657200000L,1733), (1420660800000L,730), (1420664400000L,109), (1420668000000L,95), (1420686000000L,892), (1420689600000L,6), (1420693200000L,2414), (1420696800000L,62), (1420700400000L,6), (1420707600000L,4), (1420711200000L,499), (1420714800000L,1324), (1420722000000L,82), (1420729200000L,14), (1420732800000L,206), (1420743600000L,6), (1420747200000L,1769), (1420754400000L,320), (1420758000000L,141), (1420776000000L,24), (1420779600000L,782), (1420783200000L,363), (1420786800000L,68), (1420794000000L,11), (1420801200000L,377), (1420815600000L,35), (1420819200000L,77), (1420822800000L,344), (1420826400000L,1178), (1420830000000L,119), (1420837200000L,76), (1420840800000L,295), (1420848000000L,872), (1420851600000L,4), (1420862400000L,775), (1420866000000L,91), (1420869600000L,317), (1420876800000L,109), (1420880400000L,303), (1420884000000L,300), (1420887600000L,1099), (1420891200000L,469), (1420894800000L,70), (1420898400000L,579), (1420902000000L,1035), (1420905600000L,75), (1420909200000L,83), (1420912800000L,3285), (1420916400000L,2906), (1420920000000L,163), (1420923600000L,3821), (1420927200000L,305), (1420930800000L,4250), (1420934400000L,3382), (1420938000000L,2279), (1420941600000L,2295), (1420945200000L,2272), (1420952400000L,716), (1420956000000L,193), (1420959600000L,1106), (1420963200000L,2), (1420966800000L,288), (1420970400000L,29), (1420974000000L,800), (1420977600000L,63), (1420981200000L,99), (1420984800000L,91), (1420988400000L,1665), (1420992000000L,1745), (1420995600000L,1504), (1420999200000L,429), (1421002800000L,4597), (1421006400000L,3244), (1421010000000L,995), (1421013600000L,1603), (1421017200000L,1235), (1421020800000L,1258), (1421024400000L,422), (1421028000000L,1759), (1421035200000L,380), (1421038800000L,802), (1421042400000L,720), (1421046000000L,4), (1421049600000L,114), (1421053200000L,153), (1421056800000L,26), (1421060400000L,516), (1421067600000L,6), (1421071200000L,134), (1421074800000L,212), (1421082000000L,47), (1421085600000L,1347), (1421089200000L,181), (1421092800000L,190), (1421096400000L,577), (1421107200000L,642), (1421110800000L,6), (1421114400000L,3102), (1421118000000L,2724), (1421125200000L,12), (1421128800000L,1016), (1421132400000L,649), (1421143200000L,1230), (1421146800000L,410), (1421154000000L,6), (1421157600000L,159), (1421161200000L,21), (1421164800000L,459), (1421172000000L,90), (1421175600000L,300), (1421179200000L,993), (1421200800000L,6), (1421211600000L,689), (1421215200000L,245), (1421222400000L,49), (1421233200000L,1010), (1421236800000L,77), (1421244000000L,49), (1421247600000L,17), (1421251200000L,8), (1421254800000L,35), (1421262000000L,778), (1421265600000L,8), (1421269200000L,686), (1421272800000L,156), (1421283600000L,418), (1421287200000L,40), (1421290800000L,157), (1421294400000L,197), (1421298000000L,2696), (1421301600000L,2088), (1421305200000L,891), (1421308800000L,2), (1421312400000L,246), (1421316000000L,205), (1421319600000L,27), (1421323200000L,125), (1421326800000L,1329), (1421334000000L,22), (1421337600000L,8), (1421341200000L,857), (1421352000000L,136), (1421355600000L,521), (1421359200000L,261), (1421362800000L,564), (1421366400000L,1463), (1421370000000L,12), (1421373600000L,125), (1421384400000L,1119), (1421388000000L,48), (1421391600000L,178), (1421395200000L,707), (1421398800000L,273), (1421402400000L,234), (1421420400000L,59), (1421424000000L,492), (1421427600000L,207), (1421431200000L,6), (1421434800000L,204), (1421438400000L,3321), (1421442000000L,3950), (1421445600000L,2520), (1421449200000L,6428), (1421452800000L,8038), (1421456400000L,3423), (1421460000000L,3038), (1421463600000L,659), (1421470800000L,534), (1421478000000L,4), (1421481600000L,6), (1421485200000L,202), (1421488800000L,353), (1421492400000L,424), (1421503200000L,6), (1421506800000L,553), (1421510400000L,730), (1421517600000L,3782), (1421521200000L,1682), (1421524800000L,5948), (1421528400000L,2160), (1421532000000L,3005), (1421535600000L,2068), (1421539200000L,1039), (1421542800000L,710), (1421546400000L,2432), (1421550000000L,660), (1421553600000L,1562), (1421557200000L,302), (1421564400000L,483), (1421568000000L,1819), (1421578800000L,2), (1421586000000L,237), (1421593200000L,512), (1421596800000L,68), (1421600400000L,4410), (1421604000000L,1138), (1421607600000L,4838), (1421611200000L,977), (1421614800000L,1621), (1421618400000L,1847), (1421622000000L,606), (1421625600000L,543), (1421629200000L,749), (1421632800000L,308), (1421640000000L,2911), (1421643600000L,651), (1421647200000L,61), (1421650800000L,401), (1421654400000L,513), (1421658000000L,4), (1421661600000L,389), (1421665200000L,61), (1421672400000L,908), (1421676000000L,91), (1421679600000L,247), (1421683200000L,3731), (1421686800000L,1712), (1421690400000L,2631), (1421694000000L,4985), (1421697600000L,631), (1421701200000L,4296), (1421704800000L,5488), (1421708400000L,3360), (1421712000000L,1285), (1421715600000L,3497), (1421726400000L,6), (1421730000000L,1353), (1421737200000L,1972), (1421740800000L,661), (1421744400000L,2), (1421748000000L,1223), (1421751600000L,798), (1421755200000L,352), (1421758800000L,172), (1421762400000L,838), (1421766000000L,761), (1421773200000L,6), (1421776800000L,1730), (1421780400000L,1377), (1421784000000L,14), (1421787600000L,2888), (1421791200000L,667), (1421794800000L,379), (1421798400000L,1364), (1421805600000L,2318), (1421816400000L,4), (1421820000000L,69), (1421823600000L,174), (1421830800000L,618), (1421834400000L,481), (1421838000000L,1080), (1421841600000L,1074), (1421845200000L,1311), (1421848800000L,1394), (1421852400000L,228), (1421856000000L,10), (1421859600000L,1616), (1421866800000L,879), (1421870400000L,8), (1421874000000L,1710), (1421877600000L,512), (1421881200000L,533), (1421902800000L,8), (1421906400000L,182), (1421910000000L,506), (1421913600000L,4))

}
