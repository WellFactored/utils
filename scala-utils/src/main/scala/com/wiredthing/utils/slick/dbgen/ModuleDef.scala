package com.wiredthing.utils.slick.dbgen

import shapeless.Typeable

case class ModuleDef(name: String, tables: Seq[TableGen] = Seq(), dependsOn: Seq[ModuleDef] = Seq()) {
  def generate: Seq[String] = {
    val selfTypes: Seq[String] = "DBBinding" +: "MappedTypes" +: dependsOn.map(_.name)
    val head = Seq(
      s"trait $name {",
      s"  self: ${selfTypes.mkString(" with ")} =>",
      s"  import driver.api._"
    )

    val tableDefs = tables.flatMap("" +: _.genTable().map("  " + _))

    val foot = Seq("}")

    (head :: tableDefs :: foot :: Nil).flatten
  }

  def withTableFor[T](implicit ty: Typeable[T], ti:TableInfo[T]) = copy(tables = (new TableGenerator[T] +: tables).reverse)

  def dependsOn(mods: ModuleDef*): ModuleDef = copy(dependsOn = (mods ++: dependsOn).reverse)
}
