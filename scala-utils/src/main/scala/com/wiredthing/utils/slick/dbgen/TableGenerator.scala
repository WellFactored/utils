package com.wiredthing.utils.slick.dbgen

import com.wiredthing.utils.slick.IdType
import shapeless._
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.{Keys, Values}

trait StringOps {
  def decamelise(s: String) = s.replaceAll("([a-z])([A-Z])", "$1_$2")

  def lowerCaseFirst(s: String): String = s.substring(0, 1).toLowerCase + s.substring(1)

  def stripFromEnd(s: String, count: Int) = s.substring(0, s.length - count)
}

trait TableGen {
  def genTable(): Seq[String]
}

object TableGenerator extends StringOps {
  implicit def idTypeTypeable[T](implicit tt: Typeable[T]) = new Typeable[IdType[T]] {
    override def cast(t: Any): Option[IdType[T]] = t match {
      case _: IdType[_] => Some(t.asInstanceOf[IdType[T]])
      case _ => None
    }

    override def describe: String = {
      val rowTypeName = tt.describe
      if (rowTypeName.endsWith("Row")) stripFromEnd(rowTypeName, 3) + "Id"
      else s"IdType[${tt.describe}]"
    }
  }
}

trait TableInfo[T] {
  def namesAndTypes: List[TableColumn[_]]
}

trait TableGeneratorGen {
  implicit def generator[T, R <: HList, KO <: HList, K, KLub, VO <: HList](implicit
                                                                           lgen: LabelledGeneric.Aux[T, R],
                                                                           keys: Keys.Aux[R, KO],
                                                                           values: Values.Aux[R, VO],
                                                                           fold: FoldTypes[VO],
                                                                           travK: ToTraversable.Aux[KO, List, KLub]): TableInfo[T] = {
    new TableInfo[T] {
      override def namesAndTypes: List[TableColumn[_]] = {
        val names = keys().toList.asInstanceOf[List[Symbol]].map(_.name)
        val types = fold()
        names.zip(types).map { case (n, t) => TableColumn(n, t) }
      }
    }
  }
}

class TableGenerator[T](implicit ty: Typeable[T], ti:TableInfo[T]) extends TableGen with StringOps  with TableGeneratorGen{
  lazy val namesAndTypes: List[TableColumn[_]] = ti.namesAndTypes
  val name = ty.describe
  val root = stripFromEnd(name, 3)
  val tableSQLName = decamelise(root).toUpperCase
  val tableClassName = s"${root}Table"
  val classDef = s"""class $tableClassName(tag: Tag) extends Table[$name](tag, "$tableSQLName")"""

  def generateDefsForColumn(col: TableColumn[_]): Seq[String] = {
    val colOpts = if (col.opts.isEmpty) "" else s""", ${col.opts.mkString(", ")}"""
    val colDef = s"""def ${col.n} = column[${col.ty.describe}]("${col.sqlName}"$colOpts)"""

    if (col.n.endsWith("Id")) {
      val indexRoot = stripFromEnd(col.sqlName, 3)
      val fkSQLName = s"${root.toLowerCase}_${indexRoot.toLowerCase}_fk"
      val idxSQLName = s"${root.toLowerCase}_${indexRoot.toLowerCase}_idx"
      val idStripped = stripFromEnd(col.n, 2)
      val identifierRoot = lowerCaseFirst(idStripped)
      val fk = s"""def $identifierRoot = foreignKey("$fkSQLName", ${col.n}, ${identifierRoot + "Table"})(_.id, onDelete = ForeignKeyAction.Cascade)"""
      val index = s"""def ${identifierRoot}Index = index("$idxSQLName", ${col.n})"""
      Seq(colDef, fk, index)
    } else Seq(colDef)
  }


  lazy val genTable: Seq[String] = {
    val colDefs = namesAndTypes.flatMap(generateDefsForColumn)


    val typeMappers = namesAndTypes.filter(_.needsTypeMapper).map(_.typeMapper)

    val starDef = s"def * = (${namesAndTypes.map(_.n).mkString(", ")}) <> ($name.tupled, $name.unapply)"

    Seq(
      Seq(typeMappers: _*),
      Seq(queryAlias, classDef + " {"),
      Seq(colDefs.map(d => "    " + d): _*),
      Seq("    " + starDef, "}", tableVal)
    ).flatten
  }


  lazy val tableVal = s"lazy val ${lowerCaseFirst(root)}Table = TableQuery[$tableClassName]"

  val queryAlias = s"type ${root}Query = Query[${root}Table, $name, Seq]"

}



