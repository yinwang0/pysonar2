require 'ripper'
require 'pp'
require 'json'


# --------------------- utils ---------------------
def banner(s)
  puts "\033[93m#{s}:\033[0m"
end


class AstSimplifier

  def initialize(filename)
    @filename = filename

    f = File.open(filename, 'rb')
    @src = f.read
    f.close

    @line_starts = [0]
    find_line_starts
  end


  # initialize the @line_starts array
  # used to convert (line,col) location to (start,end)
  def find_line_starts
    lines = @src.split(/\n/)
    total = 0
    lines.each { |line|
      total += line.length + 1 # line and \n
      @line_starts.push(total)
    }
  end


  def node_start(loc)
    line = loc[0]
    col = loc[1]
    @line_starts[line-1] + col
  end


  def ident_end(start_idx)
    idx = start_idx
    while @src[idx].match /[a-zA-Z0-9_]/
      idx += 1
    end
    idx
  end


  def simplify
    tree = Ripper::SexpBuilder.new(@src).parse

    banner 'sexp'
    pp tree
    simplified = convert(tree)
    simplified = convert_locations(simplified)

    banner 'simplified'
    pp simplified
    simplified
  end


  def convert_locations(obj)
    if obj.is_a?(Hash)
      new_hash = {}

      obj.each do |k, v|
        if k == :location
          start_idx = node_start(v)
          end_idx = ident_end(start_idx)
          new_hash[:start] = start_idx
          new_hash[:end] = end_idx
        else
          new_hash[k] = convert_locations(v)
        end
      end

      new_hash

    elsif obj.is_a?(Array)
      obj.map { |x| convert_locations(x) }
    else
      obj
    end

  end


  # ------------------- conversion --------------------
  # convert and simplify ruby's "sexp" into a hash
  # exp -> hash
  def convert(exp)
    if exp == nil
      {}
    else
      case exp[0]
        when :program
          {
              :type => :program,
              :body => convert(exp[1]),
              :filename => @filename
          }
        when :module
          {
              :type => :module,
              :name => convert(exp[1]),
              :body => convert(exp[2]),
              :filename => @filename
          }
        when :@ident
          {
              :type => :name,
              :id => exp[1],
              :location => exp[2],
          }
        when :@gvar
          {
              :type => :gvar,
              :id => exp[1],
              :location => exp[2]
          }
        when :symbol
          sym = convert(exp[1])
          sym[:type] = :symbol
          sym
        when :@ivar
          {
              :type => :ivar,
              :id => exp[1],
              :location => exp[2]
          }
        when :@const, :@kw
          #:@const and :@kw are just names
          {
              :type => :name,
              :id => exp[1],
              :location => exp[2]
          }
        when :def
          {
              :type => :def,
              :name => convert(exp[1]),
              :params => convert(exp[2]),
              :body => convert(exp[3])
          }
        when :do_block
          {
              :type => :funblock,
              :params => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :brace_block
          {
              :type => :funblock,
              :params => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :params
          ret = {:type => :params}
          if exp[1]
            ret[:positional] = convert_array(exp[1])
          end
          if exp[2]
            # ret[:keyword] = exp[2].map { |x| make_keyword(x) }
            exp[2].each { |x| ret[:positional].push(convert(x[0])) }
            ret[:defaults] = exp[2].map { |x| convert(x[1]) }
          end
          if exp[3]
            ret[:rest] = convert(exp[3])
          end
          if exp[4]
            ret[:after_rest] = convert_array(exp[4])
          end

          if exp[7]
            ret[:block] = convert(exp[7])
          end
          ret
        when :block_var
          params = convert(exp[1])
          if exp[2]
            params[:block_var] = convert_array(exp[2])
          end
          params
        when :class
          ret = {
              :type => :class,
              :name => convert(exp[1]),
              :body => convert(exp[3])
          }
          if exp[2]
            ret[:super] = convert(exp[2])
          end
          ret
        when :method_add_block
          call = convert(exp[1])
          call[:block_arg] = convert(exp[2])
          call
        when :method_add_arg
          call = convert(exp[1])
          call[:args] = convert(exp[2])
          call
        when :command
          {
              :type => :call,
              :func => convert(exp[1]),
              :args => convert(exp[2])
          }
        when :command_call
          if exp[2] == :'.' or exp[2] == :'::'
            func = {
                :type => :attribute,
                :value => convert(exp[1]),
                :attr => convert(exp[3])
            }
          else
            func = convert(exp[1])
          end
          {
              :type => :call,
              :func => func,
              :args => convert(exp[4])
          }
        when :call, :fcall, :super
          if exp[2] == :'.' or exp[2] == :'::'
            func = {
                :type => :attribute,
                :value => convert(exp[1]),
                :attr => convert(exp[3])
            }
          else
            func = convert(exp[1])
          end
          {
              :type => :call,
              :func => func,
          }
        when :args_new
          {
              :type => :args,
              :positional => []
          }
        when :args_add, :mrhs_add
          args = convert(exp[1])
          args[:positional].push(convert(exp[2]))
          args
        when :args_add_star
          args = convert(exp[1])
          if exp[2]
            args[:star] = convert(exp[2])
          end
          args
        when :args_add_block
          args = convert(exp[1])
          if exp[2]
            args[:block] = convert(exp[2])
          end
          args
        when :assign
          {
              :type => :assign,
              :target => convert(exp[1]),
              :value => convert(exp[2])
          }
        when :opassign
          # convert x+=1 into x=x+1
          operation = convert([:binary, exp[1], exp[2][1][0..-2], exp[3]])
          {
              :type => :assign,
              :target => convert(exp[1]),
              :value => operation
          }
        when :dot2, :dot3
          {
              :type => exp[0],
              :from => convert(exp[1]),
              :to => convert(exp[2])
          }
        when :alias
          {
              :type => :alias,
              :name1 => convert(exp[1]),
              :name2 => convert(exp[2])
          }
        when :undef
          {
              :type => :undef,
              :names => convert_array(exp[1]),
          }
        when :if, :if_mod, :elsif
          ret = {
              :type => :if,
              :test => convert(exp[1]),
              :body => convert(exp[2])
          }
          if exp[3]
            ret[:else] = convert(exp[3])
          end
          ret
        when :case
          ret = {
              :clauses => convert(exp[2])
          }
          if exp[1]
            ret[:expr] = convert(exp[1])
          end
          ret
        when :when
          {
              :type => :when,
              :pattern => convert(exp[1]),
              :value => convert(exp[2]),
              :else => convert(exp[3])
          }
        when :while, :while_mod
          {
              :type => :while,
              :test => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :unless, :unless_mod
          # to be converted to 'if not test ...'
          ret = {
              :type => :unless,
              :test => convert(exp[1]),
              :body => convert(exp[2])
          }
          if exp[3]
            ret[:else] = convert(exp[3])
          end
          ret
        when :until, :until_mod
          # to be converted to 'while not test ...'
          {
              :type => :until,
              :test => convert(exp[1]),
              :body => convert(exp[2])
          }
        when :for
          {
              :type => :for,
              :target => convert(exp[1]),
              :iter => convert(exp[2]),
              :body => convert(exp[3])
          }
        when :begin
          bodystmt = exp[1]
          {
              :type => :begin,
              :body => convert(bodystmt[1]),
              :rescue => convert(bodystmt[2]),
              :else => convert(bodystmt[3]),
              :ensure => convert(bodystmt[4])
          }
        when :rescue
          ret = {:type => :rescue}
          if exp[1]
            if exp[1][0].is_a? Array
              ret[:exceptions] = convert_array(exp[1])
            else
              exceptions = convert(exp[1])
              ret[:expections] = exceptions[:positional]
            end
          end
          if exp[2]
            ret[:binder] = convert(exp[2])
          end
          if exp[3]
            ret[:handler] = convert(exp[3])
          end
          if exp[4]
            ret[:else] = convert(exp[4])
          end
          ret
        when :stmts_new
          {
              :type => :block,
              :stmts => []
          }
        when :stmts_add
          block = convert(exp[1])
          stmt = convert(exp[2])
          block[:stmts].push(stmt)
          block
        when :binary
          {
              :type => :binary,
              :left => convert(exp[1]),
              :op => op(exp[2]),
              :right => convert(exp[3])
          }
        when :array
          args = convert(exp[1])
          {
              :type => :array,
              :elts => args[:positional]
          }
        when :aref, :aref_field
          args = convert(exp[2])
          {
              :type => :subscript,
              :value => convert(exp[1]),
              :slice => args[:positional]
          }
        when :unary
          {
              :type => :unary,
              :op => op(exp[1]),
              :operand => convert(exp[2])
          }
        when :@int
          {
              :type => :int,
              :value => exp[1],
              :location => exp[2]
          }
        when :@float
          {
              :type => :float,
              :value => exp[1],
              :location => exp[2]
          }
        when :regexp_literal
          regexp = convert(exp[1])
          regexp[:end] = convert(exp[2])
          regexp
        when :regexp_add
          {
              :type => :regexp,
              :pattern => convert(exp[2]),
          }
        when :@regexp_end
          {
              :type => :string,
              :value => exp[1],
              :location => exp[2]
          }
        when :@tstring_content
          {
              :type => :string,
              :value => exp[1],
              :location => exp[2]
          }
        when :string_content
          {
              :type => :string,
              :value => []
          }
        when :string_add
          convert(exp[2])
        when :string_concat
          convert([:binary, exp[1], :string_concat, exp[2]])
        when :assoclist_from_args
          {
              :type => :hash,
              :entries => convert_array(exp[1])
          }
        when :assoc_new
          {
              :type => :assoc,
              :key => convert(exp[1]),
              :value => convert(exp[2])
          }
        when :const_path_ref
          {
              :type => :attribute,
              :value => convert(exp[1]),
              :attr => convert(exp[2])
          }
        when :void_stmt
          {
              :type => :void
          }
        when :top_const_ref, :return, :yield, :defined
          # constructs that contains one thing
          # but should keep the type
          {
              :type => exp[0],
              :value => convert(exp[1])
          }
        when :var_ref,
            :var_field,
            :const_ref,
            :vcall,
            :paren,
            :hash,
            :else,
            :ensure,
            :arg_paren,
            :bodystmt,
            :rest_param,
            :blockarg,
            :symbol_literal,
            :regexp_literal,
            :string_literal,
            :mrhs_new_from_args
          # superflous wrappers that contains one object, just remove it
          convert(exp[1])
        else
          banner('unknown')
          puts "#{exp}"
          exp
      end
    end
  end


  def convert_array(arr)
    arr.map { |x| convert(x) }
  end


  def make_keyword(arr)
    {
        :type => :keyword,
        :key => convert(arr[0]),
        :value => convert(arr[1])
    }
  end


  def op(name)
    {
        :type => :op,
        :name => name
    }
  end

end


def parse_dump(input, output, endmark)
  simplifier = AstSimplifier.new(input)
  hash = simplifier.simplify

  json_string = JSON.pretty_generate(hash)
  out = File.open(output, 'wb')
  out.write(json_string)
  out.close

  end_file = File.open(endmark, 'wb')
  end_file.close
end


if ARGV.length > 0
  parse_dump(ARGV[0], ARGV[1], ARGV[2])
end

