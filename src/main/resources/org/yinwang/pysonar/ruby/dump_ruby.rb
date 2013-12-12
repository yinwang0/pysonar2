require 'ripper'
require 'pp'
require 'json'


# --------------------- utils ---------------------
def banner(s)
  puts "\033[93m#{s}:\033[0m"
end


class SexpSimplifier

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
    elsif exp[0] == :program
      {
          :type => :program,
          :body => convert(exp[1]),
          :filename => @filename
      }
    elsif exp[0] == :module
      {
          :type => :module,
          :name => convert(exp[1]),
          :body => convert(exp[2]),
          :filename => @filename
      }
    elsif exp[0] == :@ident
      {
          :type => :name,
          :id => exp[1],
          :location => exp[2],
      }
    elsif exp[0] == :@gvar
      {
          :type => :gvar,
          :id => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :symbol
      sym = convert(exp[1])
      sym[:type] = :symbol
      sym
    elsif exp[0] == :@ivar
      {
          :type => :ivar,
          :id => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :@const
      #:@const is just a name
      {
          :type => :name,
          :id => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :def
      {
          :type => :def,
          :name => convert(exp[1]),
          :params => convert(exp[2]),
          :body => convert(exp[3])
      }
    elsif exp[0] == :do_block
      {
          :type => :funblock,
          :params => convert(exp[1]),
          :body => convert(exp[2])
      }
    elsif exp[0] == :brace_block
      {
          :type => :funblock,
          :params => convert(exp[1]),
          :body => convert(exp[2])
      }
    elsif exp[0] == :params
      ret = {:type => :params}
      if exp[1]
        ret[:positional] = convert_array(exp[1])
      end
      if exp[2]
        # ret[:keyword] = exp[2].map { |x| make_keyword(x) }
        exp[2].each { |x| ret[:positional].push(convert(x[0])) }
        ret[:defaults] = exp[2].map { |x| convert(x[1])}
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
    elsif exp[0] == :block_var
      params = convert(exp[1])
      if exp[2]
        params[:block_var] = convert_array(exp[2])
      end
      params
    elsif exp[0] == :class
      ret = {
          :type => :class,
          :name => convert(exp[1]),
          :body => convert(exp[3])
      }
      if exp[2]
        ret[:super] = convert(exp[2])
      end
      ret
    elsif exp[0] == :method_add_block
      call = convert(exp[1])
      call[:block_arg] = convert(exp[2])
      call
    elsif exp[0] == :method_add_arg
      call = convert(exp[1])
      puts "call: #{call}"
      puts "args: #{convert(exp[2])}"
      call[:args] = convert(exp[2])
      call
    elsif exp[0] == :command
      {
          :type => :call,
          :func => convert(exp[1]),
          :args => convert(exp[2])
      }
    elsif exp[0] == :command_call
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
    elsif [:call, :fcall, :super].include?(exp[0])
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
    elsif exp[0] == :args_new
      {
          :type => :args,
          :positional => []
      }
    elsif exp[0] == :args_add
      args = convert(exp[1])
      args[:positional].push(convert(exp[2]))
      args
    elsif exp[0] == :args_add_star
      args = convert(exp[1])
      if exp[2]
        args[:star] = convert(exp[2])
      end
      args
    elsif exp[0] == :args_add_block
      args = convert(exp[1])
      if exp[2]
        args[:block] = convert(exp[2])
      end
      args
    elsif exp[0] == :assign
      {
          :type => :assign,
          :target => convert(exp[1]),
          :value => convert(exp[2])
      }
    elsif exp[0] == :opassign
      # convert x+=1 into x=x+1
      operation = convert([:binary, exp[1], exp[2][1][0..-2], exp[3]])
      {
          :type => :assign,
          :target => convert(exp[1]),
          :value => operation
      }
    elsif exp[0] == :dot2 or exp[0] == :dot3
      {
          :type => exp[0],
          :from => convert(exp[1]),
          :to => convert(exp[2])
      }
    elsif exp[0] == :alias
      {
          :type => :alias,
          :name1 => convert(exp[1]),
          :name2 => convert(exp[2])
      }
    elsif exp[0] == :undef
      {
          :type => :undef,
          :names => convert_array(exp[1]),
      }
    elsif [:if, :if_mod, :elsif].include?(exp[0])
      ret = {
          :type => :if,
          :test => convert(exp[1]),
          :body => convert(exp[2])
      }
      if exp[3]
        ret[:else] = convert(exp[3])
      end
      ret
    elsif exp[0] == :case
      ret = {
          :clauses => convert(exp[2])
      }
      if exp[1]
        ret[:expr] = convert(exp[1])
      end
      ret
    elsif exp[0] == :when
      {
          :type => :when,
          :pattern => convert(exp[1]),
          :value => convert(exp[2]),
          :else => convert(exp[3])
      }
    elsif exp[0] == :while or exp[0] == :while_mod
      {
          :type => :while,
          :test => convert(exp[1]),
          :body => convert(exp[2])
      }
    elsif exp[0] == :unless or exp[0] == :unless_mod
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
    elsif exp[0] == :until or exp[0] == :until_mod
      # to be converted to 'while not test ...'
      {
          :type => :until,
          :test => convert(exp[1]),
          :body => convert(exp[2])
      }
    elsif exp[0] == :for
      {
          :type => :for,
          :target => convert(exp[1]),
          :iter => convert(exp[2]),
          :body => convert(exp[3])
      }
    elsif exp[0] == :begin
      bodystmt = exp[1]
      {
          :type => :begin,
          :body => convert(bodystmt[1]),
          :rescue => convert(bodystmt[2]),
          :else => convert(bodystmt[3]),
          :ensure => convert(bodystmt[4])
      }
    elsif exp[0] == :rescue
      {
          :type => :rescue,
          :exceptions => convert_array(exp[1]),
          :handler => convert(exp[3])
      }
    elsif exp[0] == :stmts_new
      {
          :type => :block,
          :stmts => []
      }
    elsif exp[0] == :stmts_add
      block = convert(exp[1])
      stmt = convert(exp[2])
      block[:stmts].push(stmt)
      block
    elsif exp[0] == :binary
      {
          :type => :binary,
          :left => convert(exp[1]),
          :op => op(exp[2]),
          :right => convert(exp[3])
      }
    elsif exp[0] == :unary
      {
          :type => :unary,
          :op => op(exp[1]),
          :operand => convert(exp[2])
      }
    elsif exp[0] == :@int
      {
          :type => :int,
          :value => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :@float
      {
          :type => :float,
          :value => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :regexp_literal
      regexp = convert(exp[1])
      regexp[:end] = convert(exp[2])
      regexp
    elsif exp[0] == :regexp_add
      {
          :type => :regexp,
          :pattern => convert(exp[2]),
      }
    elsif exp[0] == :@regexp_end
      {
          :type => :string,
          :value => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :@tstring_content
      {
          :type => :string,
          :value => exp[1],
          :location => exp[2]
      }
    elsif exp[0] == :string_content
      {
          :type => :string,
          :value => []
      }
    elsif exp[0] == :string_add
      convert(exp[2])
    elsif exp[0] == :string_concat
      convert([:binary, exp[1], :string_concat, exp[2]])
    elsif exp[0] == :hash
      {
          :type => :hash,
          :value => convert(exp[1])
      }
    elsif exp[0] == :assoclist_from_args
      {
          :type => :assoclist,
          :data => convert_array(exp[1])
      }
    elsif exp[0] == :assoc_new
      {
          :type => :assoc,
          :key => exp[1],
          :value => exp[2]
      }
    elsif exp[0] == :const_path_ref
      {
          :type => :attribute,
          :value => convert(exp[1]),
          :attr => convert(exp[2])
      }
    elsif exp[0] == :void_stmt
      {
          :type => :void
      }
    elsif [:top_const_ref, :return, :yield, :defined].include?(exp[0])
      # constructs that contains one thing
      # but should keep the type
      {
          :type => exp[0],
          :value => convert(exp[1])
      }
    elsif is_wrapper?(exp[0])
      # superflous wrappers that contains one object, just remove it
      convert(exp[1])
    else
      banner('unknown')
      puts "#{exp}"
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


  def is_wrapper?(x)
    wrappers = [:var_ref,
                :var_field,
                :const_ref,
                :vcall,
                :paren,
                :else,
                :ensure,
                :arg_paren,
                :bodystmt,
                :rest_param,
                :blockarg,
                :symbol_literal,
                :regexp_literal,
                :string_literal
    ]
    wrappers.include?(x)
  end

end


def parse_dump(input, output, endmark)
  simplifier = SexpSimplifier.new(input)
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

